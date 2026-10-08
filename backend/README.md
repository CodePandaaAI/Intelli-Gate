# IntelliGate backend

This is your teammate's Django project adapted to the Android Firebase contract.
The original archive is unchanged. Its SQLite database was copied to db.sqlite3.
The old bundled Python environment was not copied; .venv is a fresh local environment.

## What runs

Android writes a Base64 JPEG to scan_images/{uid}/{scanId}. The worker listens,
decodes/validates the image, sends the JPEG to OCR.space, identifies one plate,
looks it up in Django's SQLite Vehicle table, and writes scan_results/{uid}/{scanId}.
No Android HTTP endpoint or Ktor client is needed.

The worker starts cleanup on a separate thread, so slow OCR cannot block cleanup.
Run only one worker; it starts cleanup automatically.

## Setup (PowerShell)

From the backend folder, with Python 3.12 or newer:

```powershell
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe manage.py migrate
```

The current workspace already has the environment/dependencies installed.
Publish ../firebase/database.rules.json in Firebase Realtime Database and keep
Anonymous Authentication enabled for the phone app.

Set these in the terminal that will run the worker (values are placeholders):

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\private\intelli-gate-service-account.json'
$env:FIREBASE_DATABASE_URL = 'https://intelli-gate-default-rtdb.asia-southeast1.firebasedatabase.app'
$env:OCR_SPACE_API_KEY = 'your-own-ocr-space-api-key'
.venv/Scripts/python.exe manage.py run_scan_worker
```

Get the backend service-account file from Firebase Project settings > Service accounts.
This is different from Android's google-services.json. Keep the private file outside
the shared project. Get your own OCR key at https://ocr.space/ocrapi; there is no
public example-key fallback. Environment variables are read directly; .env files
are not automatically loaded. Never paste keys into source or chat.

Leave that terminal running while testing the phone. Ctrl+C stops the worker.
Starting the Django web server alone does not start the Firebase worker.

## Vehicle records and policy

In a second terminal, from this folder:

```powershell
.venv/Scripts/python.exe manage.py createsuperuser
.venv/Scripts/python.exe manage.py runserver
```

Open http://127.0.0.1:8000/admin/ to add/edit vehicles. Set DJANGO_SECRET_KEY to
a stable private value if you want admin sessions to survive server restarts.
Without it, a temporary random key is generated for local development.

Required fields: plate_number, owner_name, role, start_date, expiry_date.
Plates are normalized on save and backwards date ranges are rejected.
Approval means registered AND start_date <= today's Asia/Kolkata date <= expiry_date.
This applies to students, faculty and staff. Monthly payments/exemptions are not
implemented, and has_pass is omitted from responses. Do not maintain a second
vehicle list in Firebase; SQLite is the source of truth for this backend.

Optional example data: `.venv/Scripts/python.exe seed_data.py` adds missing demo
plates and leaves existing records untouched. Existing expired examples do not
automatically renew; edit dates in the admin panel when preparing your demo.

## Responses and cleanup

Successful recognition returns status=complete, plate_number and Boolean is_valid.
Known vehicles also include name and lowercase role. Unsupported/no/multiple plate
candidates return status=review. OCR/network/provider errors return status=error.
The result always copies created_at from the request for expiry cleanup.

The worker deletes the photo when publishing the final result. The app attempts to
delete both paths when its scan ends. Cleanup checks both trees every five seconds
and deletes records older than 60 seconds, including results left by disconnected
phones. A result publication can race with cancellation; its original timestamp
still ensures eventual expiry. Network/service downtime can delay cleanup.
The laptop clock should be synchronized because expiry uses its current time.

SQLite AccessLog keeps text-only verification history. These are separate from the
temporary Firebase scan rows and are not deleted by the 60-second cleanup.

## Local tester and checks

http://127.0.0.1:8000/ retains the browser tester. It accepts JPEGs up to 1 MB and
2048 pixels and uses the same verification logic. It bypasses Firebase and therefore
cannot prove phone/Firebase integration. It needs the OCR key in the web-server terminal.
The former hardcoded X-Api-Key was removed; normal browser CSRF protection remains.

```powershell
.venv/Scripts/python.exe manage.py check
```

The added automated tests passed and were then removed at your request.
For a live check: run the worker, scan a registered plate with the phone, verify the
result, then stop the worker and confirm the phone times out. Restart the worker to
clear any abandoned rows. Test unknown/expired plates and unreadable photos too.

## Files to understand

- Main/verification.py: JPEG validation, OCR request, plate extraction, vehicle decision.
- Main/firebase_worker.py: stream events, queue, request decoding and processing.
- Main/management/commands/run_scan_worker.py: startup and cleanup thread.
- cleanup.py: publication and expiry deletion helpers.
- Main/models.py and Main/admin.py: the original vehicle database/admin.

Share source and requirements.txt; exclude .venv, __pycache__, credentials and local
database files unless you intentionally want to share their contents.
