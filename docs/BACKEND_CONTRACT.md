# Simple request / response contract

Use Firebase **Realtime Database**. There are two paths, sharing the same scan ID.
The anonymous user UID groups each device's records so devices cannot read one another's photos/results.

## 1. Android writes the photo

`scan_images/{user_uid}/{scan_id}`

```json
{
  "image": "<Base64-encoded JPEG>",
  "created_at": 1791450000000
}
```

- The ID is the database key; do not repeat it inside the record.
- Decode image with `base64.b64decode(value, validate=True)`. Validate the actual image before OCR.
- JPEG is at most 1,000,000 bytes (1,333,336 Base64 characters), with corrected orientation.
- The whole photo is kept. It is resized to a maximum 2048-pixel edge without upscaling.
- JPEG quality tries 90, 85, then 80. If none fits, Android asks for another photo.
- created_at is a Firebase server timestamp in milliseconds. It lets the worker discard abandoned scans.
- Capture now sends automatically; there is no separate photo editor or confirmation screen.

## 2. Python writes the answer

`scan_results/{user_uid}/{scan_id}`

```json
{
  "created_at": 1791450000000,
  "status": "complete",
  "plate_number": "PB02AB1234",
  "name": "Example owner",
  "role": "student",
  "is_valid": true
}
```

- **is_valid** is the final authorization decision computed by Python. The integrated Django
  backend checks registration and start/expiry dates. Android displays it; it does not recalculate it.
- **has_pass** is optional information about a pass on record. It alone does not approve a vehicle.
  Omit it for exempt vehicles. Firebase removes fields set to null.
- **name** and **role** are optional display fields. An unregistered vehicle can omit both.
- **plate_number** and a real Boolean **is_valid** are required when status is complete.
  Never return strings such as "true".
- A denied vehicle uses complete + is_valid: false.
- An unreadable/ambiguous plate uses `{ "status": "review" }`.
- A technical failure uses `{ "status": "error" }`.
- A missing result means still waiting. There is no separate jobs tree or processing record.

Write the entire final response once. Do not construct it through separate field updates:
the app listens for the record and rejects incomplete answers.

## Small Python worker and cleanup

Run one OCR worker process for the hackathon:

1. Read pending scan_images on startup and listen for new records.
2. Ignore deletion events and deduplicate IDs already queued/in progress.
3. Skip requests older than 60 seconds.
4. Decode the JPEG, extract the plate, look up the vehicle and determine is_valid.
5. Call backend/cleanup.py's publish_result(root, uid, scan_id, response).

The helper rechecks that the request exists and is younger than 60 seconds,
then atomically writes the result and deletes the image. The result includes
created_at copied from the original request (not the completion time).
Even review/error results need this timestamp for cleanup. Android ignores it.

The integrated worker starts cleanup on an independent thread so a slow OCR call cannot block expiry.
From backend/, after setting credentials and the OCR key as described in backend/README.md:

```powershell
.venv/Scripts/python.exe manage.py run_scan_worker
```

No separate cleanup process is needed. cleanup.py contains the helpers used by the worker.

It checks both trees on startup and every five seconds, deleting records aged 60 seconds.
Old records without created_at are also removed. This is a small hackathon sweeper;
it reads the scan trees, so use indexed expiry queries for a larger deployment.
Never put the service account in the Android app or commit it to the repository.

Android attempts to delete both paths after success, errors, cancellation or timeout.
It also registers onDisconnect removals before uploading. Firebase detects disconnections
asynchronously; this is not an exact 60-second timer. Client cleanup is best effort.
If the phone and cleanup process are offline, rows remain until one reconnects.
A cancellation racing with backend publication can briefly leave a late result;
the sweeper removes it using the original request timestamp.

## Android state and retries

No SavedStateHandle or process restoration. Rotation retains the ViewModel, but process
death starts a fresh scan. Each attempt/retry has a new ID. Retry can use the local photo
while this ViewModel is alive; old responses cannot satisfy the new attempt.

The scan timeout is 60 seconds from preparation; the database expiry is 60 seconds
from upload. Final best-effort deletion can add up to five seconds before timeout/error
is displayed. Cancel/reset returns home immediately. Finishing the activity deletes
local captures; after a hard process kill, leftovers are deleted when the image processor
is next created. Going to the background alone does not destroy a ViewModel.

## Vehicle list and credentials

The integrated backend keeps vehicle records in Django's SQLite Vehicle table, managed
through its admin panel. Fields are plate_number, owner_name, role, start_date and expiry_date.
Approval requires registration and a current validity interval for every role. Monthly
payments and faculty exemptions are not implemented. has_pass is omitted in its response.
Do not maintain a duplicate vehicle list in Firebase. The laptop checks dates in Asia/Kolkata.

Python uses the Firebase Admin SDK with credentials stored only on the backend machine.
Android signs in anonymously automatically. Any installation can submit scans without enrollment.
Only the backend writes results or vehicle records.

## Migrating from the previous app

- Publish the updated **firebase/database.rules.json**.
- Replace image_base64 with image, and put created_at on the image record.
- Copy that original created_at into all results and run the Django worker (cleanup starts automatically).
- Publish the new 1 MB image limit and owner-delete permissions in the rules.
- Stop using scan_jobs and direction.
- Return the response shown above instead of decision/reason/owner_name.
- The old and new response formats are intentionally not mixed.
- The current Android build must be paired with these rules and this worker contract.

