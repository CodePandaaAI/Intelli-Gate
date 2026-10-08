import os
import threading

import firebase_admin
from firebase_admin import db
from django.core.management.base import BaseCommand, CommandError

from Main.firebase_worker import ScanWorker, run_cleanup


class Command(BaseCommand):
    help = "Listen for Firebase photos, verify vehicles, and clean expired scan records. Run one instance."

    def handle(self, *args, **options):
        database_url = os.environ.get("FIREBASE_DATABASE_URL")
        if not database_url or not os.environ.get("OCR_SPACE_API_KEY"):
            raise CommandError("Set FIREBASE_DATABASE_URL and OCR_SPACE_API_KEY. See backend/README.md.")
        app = firebase_admin.initialize_app(options={"databaseURL": database_url, "httpTimeout": 20})
        stop = threading.Event()
        listener = None
        cleaner = None
        try:
            root = db.reference("/", app=app)
            worker = ScanWorker(root)
            cleaner = threading.Thread(target=run_cleanup, args=(root, stop), daemon=True)
            cleaner.start()
            listener = root.child("scan_images").listen(worker.on_event)
            self.stdout.write(self.style.SUCCESS("Listening for scans. Cleanup is running. Ctrl+C to stop."))
            worker.run(stop)
        except KeyboardInterrupt:
            self.stdout.write("Stopping scan worker.")
        finally:
            stop.set()
            if listener:
                listener.close()
            if cleaner:
                cleaner.join(timeout=25)
            firebase_admin.delete_app(app)
