# IntelliGate

A small Kotlin / Jetpack Compose app: take a number-plate photo, compress it, send it to
Firebase Realtime Database, and display the Python worker's answer.

## The app

- Minimal Material 3 Expressive screen, large scan button, light/dark themes.
- Opens the phone's default camera using TakePicture and a FileProvider URI.
- Sends the **full photo** automatically after capture. No cropping, preview editor or entry/exit selector.
- Corrects image orientation, resizes without upscaling and compresses JPEG to at most 1 MB.
- Shows approved, not approved, unreadable-plate or connection/error states.
- Koin dependency injection, one ViewModel, lifecycle-aware state collection.
- No navigation graph, Ktor, separate job queue or runtime demo settings.

Code is separated by responsibility: data, domain, ui/scan, ui/components and ui/theme.
Firebase/backend setup documentation is separate from production app code.

## Run

Requires JDK 21, Android SDK 37 and Android 11+ on the phone.
Material 3 1.5.0-beta01 is pinned for Expressive APIs.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

The project can build before Firebase is configured, but live scans require
app/google-services.json. There is no fake-approval fallback.
Previews run independently of Firebase.

## Firebase setup

1. Register Android package **com.liftley.intelligate** in your Firebase project.
2. Create Realtime Database, enable Anonymous Authentication, and download google-services.json
   to app/google-services.json (download after creating the database so its URL is included).
3. Publish **firebase/database.rules.json**. These rules were simplified along with the request format;
   republish them if you used the earlier three-tree version.
4. No device enrollment is required: anonymous sign-in happens automatically.
5. Follow **backend/README.md** to run the Django Firebase worker and add vehicle records in its admin panel.

Request: scan_images/{user_uid}/{scan_id} → image + created_at.
Response: scan_results/{user_uid}/{scan_id} → created_at, status, plate_number, name, role, has_pass, is_valid.
The backend alone decides is_valid. Missing/unreadable/error responses never grant approval.

Existing cloud rules are not changed by editing these files; publish the rules to enable access without enrollment.
Your teammate's Django backend is integrated in backend/ with a Firebase worker, OCR.space adapter,
SQLite vehicle database and admin panel. See backend/README.md for credentials and startup.

## Reliability without extra screens

Scan state lives only in the ViewModel; no SavedStateHandle or process restoration.
Every retry uses a fresh scan ID. Android attempts Firebase cleanup after each attempt
and registers disconnect cleanup. The Django worker starts an independent cleanup thread to
remove abandoned image/result records after 60 seconds (checked every five seconds).
See docs/BACKEND_CONTRACT.md for setup and the response timestamp requirement.
Local photos are removed after a result/reset or when the ViewModel is cleared;
hard-kill leftovers are removed on the next image-processor initialization.
Use one Python OCR worker for the hackathon.

Full-photo compression cannot guarantee OCR accuracy. Move close enough that the characters are clear.

