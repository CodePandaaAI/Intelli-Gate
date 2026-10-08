"""One sequential OCR worker; Firebase callbacks only enqueue request IDs."""

import base64
import binascii
import logging
import queue
import threading
import time

from django.db import close_old_connections

from cleanup import SCAN_LIFETIME_MS, publish_result, sweep_once
from .verification import verify_image


def changed_requests(path, data):
    """Handle initial snapshots, individual writes and multi-path Firebase patches."""
    parts = [part for part in path.split("/") if part]
    if data is None:
        return
    if len(parts) >= 2:
        yield tuple(parts[:2])
    elif isinstance(data, dict):
        for key, value in data.items():
            yield from changed_requests("/".join(parts + [key]), value)


class ScanWorker:
    def __init__(self, root):
        self.root = root
        self.requests = queue.Queue()
        self.pending = set()
        self.lock = threading.Lock()

    def on_event(self, event):
        if event.event_type not in ("put", "patch"):
            return
        for key in changed_requests(event.path, event.data):
            with self.lock:
                if key in self.pending:
                    continue
                self.pending.add(key)
            self.requests.put(key)

    def process(self, uid, scan_id):
        request_ref = self.root.child(f"scan_images/{uid}/{scan_id}")
        record = request_ref.get()
        if not isinstance(record, dict):
            return
        created_at = record.get("created_at")
        if (type(created_at) not in (int, float)
                or not 0 <= int(time.time() * 1000) - created_at < SCAN_LIFETIME_MS):
            request_ref.delete()
            return
        if self.root.child(f"scan_results/{uid}/{scan_id}").get() is not None:
            request_ref.delete()
            return
        try:
            encoded = record.get("image")
            if not isinstance(encoded, str) or len(encoded) > 1_333_336:
                raise ValueError("Invalid image payload")
            image_bytes = base64.b64decode(encoded, validate=True)
            response = verify_image(image_bytes)
        except (ValueError, binascii.Error):
            response = {"status": "error"}
        except Exception:
            logging.exception("Vehicle check failed for scan %s", scan_id)
            response = {"status": "error"}
        # Recheck existence/expiry after slow OCR. Never resurrect an expired image.
        publish_result(self.root, uid, scan_id, response)

    def run(self, stop):
        while not stop.is_set():
            try:
                key = self.requests.get(timeout=1)
            except queue.Empty:
                continue
            try:
                close_old_connections()
                self.process(*key)
            except Exception:
                logging.exception("Firebase request failed; expiry cleanup will remove leftovers")
            finally:
                close_old_connections()
                with self.lock:
                    self.pending.discard(key)
                self.requests.task_done()


def run_cleanup(root, stop):
    while not stop.is_set():
        try:
            sweep_once(root)
        except Exception:
            logging.exception("Cleanup failed; retrying in five seconds")
        stop.wait(5)
