"""Result publication and expiry cleanup used by the Django worker."""

import time

SCAN_LIFETIME_MS = 60_000


def expired_paths(records, tree, now_ms):
    """Results retain the request's created_at, so both expire together."""
    paths = {}
    for uid, scans in (records or {}).items():
        if not isinstance(scans, dict):
            paths[f"{tree}/{uid}"] = None
            continue
        for scan_id, record in scans.items():
            created_at = record.get("created_at") if isinstance(record, dict) else None
            if type(created_at) not in (int, float) or now_ms - created_at >= SCAN_LIFETIME_MS:
                paths[f"{tree}/{uid}/{scan_id}"] = None
    return paths


def sweep_once(root):
    now_ms = int(time.time() * 1000)
    for tree in ("scan_images", "scan_results"):
        deletions = expired_paths(root.child(tree).get(), tree, now_ms)
        if deletions:
            root.update(deletions)


def publish_result(root, uid, scan_id, response):
    """Call this after OCR; do not publish answers for missing/expired requests."""
    image_path = f"scan_images/{uid}/{scan_id}"
    request = root.child(image_path).get()
    if not isinstance(request, dict):
        return False
    created_at = request.get("created_at")
    if (type(created_at) not in (int, float)
            or not 0 <= int(time.time() * 1000) - created_at < SCAN_LIFETIME_MS):
        root.child(image_path).delete()
        return False
    root.update({
        f"scan_results/{uid}/{scan_id}": {**response, "created_at": created_at},
        image_path: None,
    })
    return True
