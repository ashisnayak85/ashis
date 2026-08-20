# Face Verification for Self-Attendance — What Was Added

## The problem this solves
Previously, anyone who had an employee's login credentials could punch
in/out "as" them. This adds an optional face-match step so a live photo is
checked against the employee's own enrolled face before a self-punch is
recorded.

## The ON/OFF switch
Single property in `src/main/resources/application.properties`:

    attendance.face-verification.enabled=false

- `false` (default): everything behaves exactly as before this feature was
  added. No camera step shown, no model loaded, no cost.
- `true`: punch-in/out requires a live photo, checked against the
  employee's enrolled face.

No code changes needed either way — just flip the value (or set the
`FACE_VERIFICATION_ENABLED` environment variable) and restart.

## Before you run it
1. `mvn clean compile` in `enterprise-employee-management/` — **this was
   not compiled/tested in the sandbox that generated it** (no Maven Central
   access there). Maven Central and `resources.djl.ai` (the free model
   download) are both normal public endpoints your machine can reach.
2. If it doesn't compile cleanly, the issue is almost certainly confined to
   `FaceFeatureTranslator` (the inner class at the bottom of
   `service/impl/FaceRecognitionServiceImpl.java`) — DJL's exact translator
   API shifts slightly between versions. Paste me the compiler error and
   I'll fix it in one pass.
3. `npm install` in `ems-frontend/` (the delivered zip has `node_modules`
   stripped out to keep the file small — this is normal and expected).

## What's new, file by file

### Backend
- `entity/FaceEnrollment.java` — one row per employee, stores their
  reference face as a numeric embedding (not a second photo).
- `entity/Attendance.java` — new `faceVerified` boolean column.
- `repository/FaceEnrollmentRepository.java`
- `dto/FaceStatusDTO.java` — `{enabled, enrolled}`, drives the frontend UI.
- `dto/AttendanceDTO.java` — added `faceVerified` (server-set only).
- `service/FaceRecognitionService.java` + `impl/FaceRecognitionServiceImpl.java`
  — the actual face-matching logic, using DJL (free, self-hosted, no API key).
- `service/AttendanceService.java` + `impl/AttendanceServiceImpl.java` —
  `punchIn`/`punchOut` now take a `faceVerified` flag, always computed
  server-side, never trusted from the client.
- `controller/api/DepartmentApiController.java` (`AttendanceApiController`)
  — punch-in/out now accept multipart with an optional photo; new
  `/self/face/status` and `/self/face/enroll` endpoints.

### Frontend
- `src/api/attendance.js` — multipart punch calls, `getFaceStatus()`,
  `enrollFace()`.
- `src/components/FaceCapture.jsx` — reusable webcam capture widget.
  Captures a photo only; the actual match always happens server-side.
- `src/pages/Attendance.jsx` — wires it together: shows an "Enroll My
  Face" prompt when the feature is on and the employee hasn't enrolled
  yet, and routes Punch In/Out through the camera step when enabled.

## Tuning after you pilot it
`attendance.face-verification.match-threshold` (default `0.75`) controls
how strict the match is. If real employees get rejected too often, lower
it slightly; if it's letting mismatches through, raise it. Test with a
handful of people across different lighting/webcams before rolling out
company-wide.

**Use the admin Test Verify tool (Attendance page → "Face Verification —
Admin", staff only) to actually see the number** instead of guessing: pick
the employee, hit Test Verify, take a photo. It shows the real similarity
score next to the threshold. A score consistently just under 0.75 across
several genuine attempts means the threshold is too strict for this model -
lower it. A score wildly lower (e.g. under 0.3) for the actual right person
usually means something else is wrong - check enrollment photo quality
first (centered face, good even lighting, no extreme angle).

## Admin face management + backup history (2nd pass)
Beyond an employee enrolling their own face, admins/managers can now:
- (Re-)enroll ANY employee's face (Attendance page → "Face Verification —
  Admin"): `POST /api/attendance/admin/face/enroll/{employeeId}`.
- Test a photo against an employee's enrolled face without affecting
  attendance: `POST /api/attendance/admin/face/test-verify/{employeeId}`.
- Check any employee's enrollment status: `GET /api/attendance/admin/face/status/{employeeId}`.
- View backup history: `GET /api/attendance/admin/face/history/{employeeId}`.

**Backups:** every time a face is re-enrolled (self or admin-triggered),
the PREVIOUS embedding + reference photo are copied into a new
`face_enrollment_history` table before being overwritten - nothing is ever
silently lost. New DB table (created automatically via `ddl-auto=update`
since it didn't exist before): `face_enrollment_history`. See
`entity/FaceEnrollmentHistory.java`.

**Also fixed in this pass:** the reference photo was never actually being
saved to disk before (`FaceEnrollment.referencePhotoPath` was always null) -
enrollment now stores the real captured photo via `FileStorageService`, so
there's something to look at during an audit, not just opaque numbers.
