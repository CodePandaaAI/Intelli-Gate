# Reading IntelliGate's non-UI code

Read these files in order: ScanRoute, ScanViewModel, PlateImageProcessor,
FirebaseVerificationRepository, ResultParser, then backend/cleanup.py.

## 1. The camera boundary: ScanRoute

TakePicture opens the installed camera app and gives it a temporary file URI to write to.
The camera returns a Boolean: did the user accept a photo? Its callback calls
onCameraResult. That is a normal UI event, like a button click; it does not save state.
The callback is necessary because the camera is a separate activity, not a suspend function.

collectAsStateWithLifecycle lets Compose observe ViewModel state while the screen is active.
The ViewModel never calls a Composable. It updates state, and Compose redraws from that state.

## 2. Memory and work: ScanViewModel

MutableStateFlow holds the current screen state (ready, preparing, uploading, processing,
result or error). asStateFlow exposes a read-only view to the UI.
cameraUri and photoUri are ordinary in-memory fields: temporary camera output and accepted photo.
They are not saved to SavedStateHandle, preferences or a database.

verify creates a new UUID for each attempt, launches a coroutine in viewModelScope,
and applies a 60-second timeout. It prepares the JPEG, then collects repository updates.
Each update changes the screen state. Failures show an error with retry; retry uses the
local photo but a new request ID. A successful result deletes the local photo.

reset cancels work and deletes the photo. onCleared deletes remaining local captures when
the ViewModel is destroyed. Rotation usually keeps that ViewModel. Backgrounding the app
does not mean it has closed. Android can kill a process without calling cleanup methods;
that is why the next image-processor initialization removes orphaned cache files.
If Android kills the process while the external camera is open, that photo is not resumed.

The camera needs a file even though scan state is in memory. A URI is a reference to that
file, not the photo's bytes. Android controls the cache; we cannot guarantee immediate
physical file deletion at the instant of a hard process kill.

## 3. Image preparation: PlateImageProcessor

withContext(Dispatchers.IO) moves decoding/compression away from the UI thread.
ImageDecoder reads the image and applies its EXIF orientation.

scale = minOf(1f, 2048f / maxOf(width, height))

For a 4000 x 3000 photo, scale is 0.512, producing 2048 x 1536.
For a 1200 x 600 photo, scale is 1: we do not enlarge it.
Both dimensions use the same scale, preserving proportions and the full frame.

bitmap.compress writes a JPEG into ByteArrayOutputStream, an expandable memory buffer.
We try quality 90, then 85, then 80, always starting from the same decoded bitmap.
These numbers are encoder quality settings, not a percentage reduction in file size.
The first result at or below 1,000,000 bytes is returned. If none fits, we ask for a retake
rather than silently reducing quality further. recycle releases the decoded pixel buffer.

The 1 MB limit is a maximum, not a target: a 180 KB image stays 180 KB.
The new 2048-pixel/1 MB settings are a practical starting point, not a measured OCR guarantee.
Test real plates in daylight, shade and motion before tuning them further.

## 4. Transport: FirebaseVerificationRepository

flow is a sequence of progress updates collected by the ViewModel.
Firebase calls often return Task objects. await suspends the coroutine until the Task
finishes without blocking the screen's thread.

The repository signs in anonymously if necessary, then builds two database references
from the user UID and request UUID. These references are addresses, not downloaded data.
It registers onDisconnect removal for both addresses before uploading.

Base64.encodeToString converts JPEG bytes into text. It is not encryption or compression;
it adds about one third to the size. A 1 MB JPEG becomes about 1.33 MB of Base64 text.
setValue uploads that text and a Firebase server timestamp as one request record.

addValueEventListener observes the matching result. It receives an initial snapshot and
later changes. No record means wait. A complete record goes to ResultParser.
callbackFlow adapts this Firebase callback API into Kotlin Flow, so callbacks stay inside
the data layer. awaitClose removes the listener when the scan ends.

The finally block attempts to delete both remote paths on success, failure or cancellation.
NonCancellable allows that short cleanup to run even after the scan is cancelled;
its own five-second timeout prevents cleanup from waiting forever. Cleanup failure is
not treated as a failed vehicle check. The server sweeper is the fallback.

## 5. Answers: ResultParser

Firebase gives us untyped values. The parser checks that status, plate_number and is_valid
have the expected types. It maps is_valid true/false into approved/denied.
review means unreadable or ambiguous; error means the backend failed to check.
Neither is silently interpreted as approval or vehicle denial.
has_pass is optional display information, not another approval calculation.

## 6. Expiry: backend/cleanup.py

Firebase rules allow or reject operations; they do not run scheduled deletion code.
The worker's independent cleanup thread reads both scan trees every five seconds and deletes expired
records. Each record uses the request's original created_at, including results.
publish_result checks that a request still exists and has not expired, copies its timestamp
into the response, and deletes the image in the same database update.

Deletion and publication can race, so a late response can briefly appear after cancellation.
The retained original timestamp ensures it still expires. Cleanup is eventual, not an exact
60-second guarantee: the cleanup process and network must be available. It runs independently
from slow OCR so image recognition cannot delay its five-second loop.

This helper does not perform OCR or check the vehicle database. Main/firebase_worker.py
decodes queued requests and calls Main/verification.py for OCR and the SQLite lookup, then
calls publish_result. See backend/README.md for startup and the exact approval policy.
