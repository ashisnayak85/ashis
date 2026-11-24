# OneHealth Clinic Platform - Backend

A multi-branch clinic booking + owner-analytics platform, built for a single
clinic-chain client ("One Health") but multi-tenant-ready under the hood
(`Organization` is a first-class entity), so a second clinic-chain customer
can be onboarded later without a schema rewrite.

**This is a separate, independent codebase** from the public
doctor-appointment-marketplace project it was forked from - different trust
model, different customer, different roles. See "How this differs from the
marketplace project" below.

## Roles

| Role          | Scope                          | What they do |
|---------------|---------------------------------|---------------|
| `SUPER_ADMIN` | Platform-wide                   | Onboards new clinic-chain customers (`Organization` + first `OWNER` login). No public registration endpoint. |
| `OWNER`       | One organization, all branches  | Creates/manages clinics, clinic-admin logins, doctors, doctor-clinic assignments; views the cross-branch analytics dashboard. |
| `CLINIC_ADMIN`| One organization, one clinic    | Books walk-ins, views/updates that branch's appointments, views that branch's own dashboard. |
| `DOCTOR`      | One organization, own schedule  | Sets weekly availability at their assigned branch(es); views/updates own appointments. |
| `PATIENT`     | One organization                | Registers under an organization (by slug), browses branches/doctors, books/cancels appointments. |

## Local setup

1. MySQL running locally; a schema `onehealth_db` will be auto-created.
2. `mvn spring-boot:run` (or run `OneHealthApplication` from your IDE). Runs on port `8083`.
3. On first boot, `DataSeeder` creates:
   - a `SUPER_ADMIN` login (`app.superadmin.email` / `app.superadmin.password`, see `application.properties`)
   - if `app.seed.demo-org=true` (default), a demo `Organization` ("One Health") + its first `OWNER` login (`app.seed.owner-email` / `app.seed.owner-password`)
4. Set `app.seed.demo-org=false` before handing this off to a real customer, so they start from a clean org list and just log in as `OWNER` after you create their org via the super-admin endpoint.

## Key flows

- **Owner sets up the org**: `POST /api/owner/clinics` (branches) → `POST /api/owner/clinic-admins` (front-desk login per branch) → `POST /api/owner/doctors` (with optional `clinicIds` to assign immediately, or `POST /api/owner/doctors/assign` later).
- **Doctor sets weekly hours**: `POST /api/doctor/availability`. Rejected if it overlaps hours the doctor already has at *any* branch of the org on the same weekday - see "Doctor can't be double-booked across branches" below.
- **Patient books online**: browses `GET /api/patient/clinics` → `GET /api/patient/clinics/{id}/doctors` → `GET /api/patient/slots` → `POST /api/patient/appointments`.
- **Walk-in booked by front desk**: `GET /api/clinic-admin/slots` → `POST /api/clinic-admin/appointments/walk-in` (with `patientId`, or `patientName`/`patientPhone` to create a lightweight patient record with no login).
- **Owner dashboard**: `GET /api/owner/dashboard?from=&to=&clinicId=`. Omit `from`/`to` to default to today; omit `clinicId` for the all-branches, location-wise comparison view.

## How this differs from the marketplace project

1. **Trust model.** The marketplace project has independent clinics and doctors that discover each other and go through a `PENDING → APPROVED/REJECTED` association workflow, moderated by a platform admin. Here, clinics and doctors both belong to the same organization, so `DoctorClinicAssignment` is a direct assignment made by the owner - no approval step needed.
2. **Multi-tenancy.** `Organization` is new. Every `Clinic`, `Doctor`, `ClinicAdmin`, `Patient`, `Appointment`, and `User` (except `SUPER_ADMIN`) carries an `organizationId`, and every service method that touches org-scoped data explicitly checks it (see `AccessDeniedBusinessException` usages) - this is the tenant-isolation boundary that lets the platform be resold to a second clinic chain later.
3. **Walk-ins.** `Appointment.source` (`ONLINE` / `WALK_IN`) and `Patient.user` (nullable) are new - a walk-in doesn't need to have ever used the app. `POST /api/clinic-admin/appointments/walk-in` handles this.
4. **Owner analytics dashboard.** `DashboardService`/`DashboardStatsDTO` are entirely new: location-wise appointment volume, completion/no-show/cancellation rates, revenue, doctor utilization, and a daily trend - all filterable by date range (defaults to today) and optionally by branch.
5. **Doctor can't be double-booked across branches.** Enforced at `DoctorAvailability` template-creation time (`DoctorAvailabilityRepository.findOverlapping` checks across *all* of the org's branches, not just the one being edited), so it's impossible to even generate colliding bookable slots for one doctor across two locations - not just a last-resort DB constraint.

## Update: bug fixes + new features (post-testing feedback)

After initial testing, several issues surfaced. Here's what changed and why.

### Bug fixes

**Root cause of "doctors not loading," "doctors not loading for a branch," and "dashboard failing to load":**
All three were the same underlying bug - `LazyInitializationException`. With `spring.jpa.open-in-view=false` (correct, deliberate setting), a Hibernate session closes the instant a repository call returns. Several read methods (`DoctorService.listDoctors`, the patient-facing "doctors at this branch" lookup, and the whole `DashboardService`) fetched an entity and *then* read a lazy-loaded related entity off it (e.g. a doctor's assigned clinic name) after that session had already closed. It worked fine on an empty database (nothing lazy to touch) and broke the moment real data with real relationships existed - which matches exactly what was reported.

Fixed by wrapping every read path that touches a lazy relation in `@Transactional(readOnly = true)`, so the session stays open for the whole computation. Also fixed a related issue: `PatientController` was returning the raw `Doctor` JPA entity directly, which carries a lazy `User` (containing the password hash) - a latent information-leak risk on top of the loading bug. That endpoint now returns `DoctorDTO` like everywhere else.

**Dashboard resilience:** Per the requirement that the dashboard is critical and one failing piece shouldn't take down the whole screen, `DashboardService.getStats` now computes each section (branch breakdown, doctor utilization, trend, revenue, unique-patient count) independently. If any one section throws, it comes back empty/zeroed with a note in the new `sectionWarnings` field on `DashboardStatsDTO`, but the rest of the dashboard still renders.

### New features

**Specialization master list.** `Doctor.specialization` (free text) was replaced with a many-to-many relationship to a new `Specialization` entity, managed by the owner (`/api/owner/specializations` - create/rename/activate/deactivate/delete). Doctors are now tagged via `specializationIds` (multi-select) instead of typed text - avoids "Cardiologist" vs "cardiologist" data fragmentation. Delete is blocked if any doctor currently has that specialization (deactivate instead); this is intentional so deleting one specialization can't silently strip it off doctors who have it.

**Staff HR profiles + salary history.** New `EmployeeProfile` entity (gender, DOB, date of joining, permanent/current address) and `SalaryRecord` entity, both reachable only via `/api/owner/employees/**` (`OwnerEmployeeController`, `hasRole('OWNER')`). Both `ClinicAdmin` and `Doctor` are treated as employees here, keyed by their `userId` rather than duplicating HR columns onto each entity - this also means adding a future staff type (e.g. a Receptionist role) doesn't require re-adding the same fields again.

Salary is modeled as an append-only history (`SalaryRecord`), not a single mutable field - a new revision is always added, never overwriting a previous one, so a raise doesn't erase what someone earned before it. "Current salary" is computed as the most recent record whose `effectiveFrom` is on or before today. Deliberately kept out of `DoctorDTO`/anything a patient or other staff member can see - salary only exists behind owner-only endpoints.

**Clinic location picker (map-based, no Google billing required).** The clinic creation form was missing the lat/long fields the backend already supported. Rather than Google Maps (which requires a Google Cloud billing account even within the free tier), the frontend now uses OpenStreetMap + Leaflet + the free Nominatim geocoder - search an address or click/drag a marker on the map to set the exact location, no API key needed. See the frontend README for the trade-off and Nominatim's usage policy.

## Update: future-slot resync when a doctor's schedule changes

Previously, if a doctor changed their weekly template (`doctor_availability`) after some future dates had already been viewed/generated (`appointment_slots`), those already-generated dates stayed frozen at whatever the template looked like when they were first materialized. Fixed: `SlotService.addAvailability`, `deleteAvailability`, and `setAvailabilityActive` now all call a new `resyncFutureSlots(doctor, clinic, dayOfWeek)` step afterward.

What it does, for every future date on that weekday that already has at least one generated slot row (dates nobody's looked at yet don't need touching - they'll simply generate correctly from the current template on first view):
- A time window the current template now covers but that date doesn't have yet → added as `AVAILABLE` (or a previously-cancelled row at that same start time is reactivated, rather than inserting a duplicate that would hit the unique constraint).
- A previously-generated `AVAILABLE` slot the current template no longer covers → marked `CANCELLED` (soft removal, not deleted - keeps the row for audit history and consistent with how cancelled appointments already work).
- A slot that's already `BOOKED` but falls outside the updated template → **left completely untouched**. A live appointment is never silently cancelled by a schedule edit; it's reported back in a `warnings` list instead so a human can decide whether to reschedule or cancel it explicitly.

`AvailabilityResponse` (from add/activate/deactivate) and the delete endpoint's response (`SlotResyncResultDTO`) both carry `futureDatesChecked` / `slotsAdded` / `slotsCancelled` / `warnings` so the frontend can tell the doctor what happened, including calling out anything that needs manual attention.

## Update: "how would anyone know a doctor/branch ID" fix

Two forms previously asked staff to type a raw numeric database ID they'd have no way of knowing:
- `POST /api/clinic-admin/appointments/walk-in` required a `doctorId` with no way for front-desk staff to look one up. Added `GET /api/clinic-admin/doctors` (`ClinicAdminController.doctorsAtMyClinic`, reusing `DoctorService.getActiveDoctorsAtClinic`) so the frontend can show a name dropdown instead.
- `POST /api/doctor/availability` required a `clinicId` with the same problem for doctors. Added `GET /api/doctor/my-clinics` (`DoctorController.myClinics`, backed by new `DoctorService.getMyAssignedClinics`) for the same reason.

Both return `DoctorDTO`/`ClinicDTO` (never raw entities), consistent with the earlier lazy-loading fix.

## Update: infinite-loop bug, missing validation, and the login-greeting bug

**Critical fix - a slot duration of 0 could hang a server thread forever.** `SlotService`'s slot-generation loop advances a cursor by `slotDurationMinutes` each iteration; a duration of 0 (or negative) meant the cursor never moved, so the loop never terminated. This wasn't just a validation nicety - it was a real denial-of-service risk (any request touching a date generated from that template would hang). Fixed in three layers: `AvailabilityRequest.slotDurationMinutes` now has `@Min(1)` so bad input is rejected with a clean 400 before reaching any logic; `SlotService.addAvailability` explicitly checks duration >= 1 and <= the window length with a clear message; and `expandTemplatesToWindows` (used by both slot generation and the resync logic) skips any template with a non-positive duration as a last line of defense, in case bad data ever gets in another way (direct DB edit, a future bulk-import script).

**Other validation gaps closed:** `RegisterDoctorRequest` (experience years 0-75, consultation fee can't be negative), `ClinicRequest` (latitude/longitude bounds), `AddSalaryRecordRequest` (amount can't be negative) all now have bean-validation annotations instead of silently accepting anything. Start-time-after-end-time on the availability form was already rejected server-side (`SlotService.addAvailability`) - what was missing was *client-side* feedback, so someone had to submit and wait for a server round-trip to find out; the frontend now checks this immediately (see frontend README).

**Login greeting showing the role instead of the person's name.** Root cause: `User.name` is only ever populated for `OWNER`/`SUPER_ADMIN` - `CLINIC_ADMIN`, `DOCTOR`, and `PATIENT` keep their name on their own profile entity (`ClinicAdmin.name`, `Doctor.name`, `Patient.name`) instead, by original design. But `AuthService.buildAuthResponse` was reading `user.getName()` directly for every role, which is `null` for those three - so the frontend's fallback (`user.name || user.role`) kicked in and displayed the role instead of the actual name. Fixed with a new shared `UserDisplayNameService`, which looks the name up from the correct table based on role (also reused by the new audit logging below, so this logic lives in exactly one place).

## Update: booking cutoff uses end time, and owner/clinic-authority can turn a time period on/off (with a history log)

**Booking cutoff changed from start time to end time.** Previously a slot stopped being bookable the instant its start time passed. Per updated requirements, a slot now stays bookable until its **end** time passes - e.g. a 9:00-9:15 slot can still be booked at 9:05. Changed in two places that have to agree: `SlotService.getOrGenerateSlots` (what's shown as bookable) and `AppointmentService.lockAndValidateSlot` (the actual gate at booking time, re-checked at the moment of booking since time can cross the cutoff between when a list was loaded and when someone clicks "book").

**Owner and clinic authority can both turn a doctor's time period on/off, with a full audit trail.** This is a different, more granular capability than the existing per-slot generation - `DoctorAvailability` (a recurring weekly window, e.g. "Dr. Sharma, Sundays, 9:00-12:00 and 14:00-18:00 at Branch A") already supported multiple windows per day; what was missing was: (1) owner-level access to the on/off toggle (previously `CLINIC_ADMIN`-only), and (2) a record of who made a change.

- `SlotService.setAvailabilityActiveForOwner` / `setAvailabilityActiveForClinicAdmin` both delegate to the same core logic (toggle + resync future slots), differing only in their authorization check - owner can toggle any window in their org, clinic authority only their own branch.
- New `AvailabilityAuditLog` entity records every `CREATED` / `DELETED` / `ACTIVATED` / `DEACTIVATED` event: who did it (denormalized name + role, resolved via `UserDisplayNameService` at the moment of the action), what time period (denormalized doctor/clinic/day/time, so the log stays readable even if the underlying `DoctorAvailability` row is later deleted), and any resync summary (e.g. "3 slots cancelled; 1 booked appointment needs manual review"). Deliberately denormalized rather than just storing foreign keys - an audit log should still make sense years later even if the thing it's describing has since changed or been removed.
- `GET /api/owner/availability` / `GET /api/clinic-admin/availability` list every time period (doctor, clinic, day, time, on/off status) for the org or branch respectively; `GET /api/owner/availability-history` / `GET /api/clinic-admin/availability-history` return the change log, newest first.

## Not included in this scaffold (intentionally, to keep the build focused)

- Doctor ratings/reviews (present in the marketplace project; drop back in if the owner wants it - straightforward to port).
- Payment gateway integration (`PaymentStatus` enum is there; wiring an actual gateway is a follow-up).
- Self-serve org signup / billing for a second customer (only matters once you actually have a second paying customer - see the multi-tenancy discussion in the project chat history).

## Update: booking cutoff changed from slot-level to window-level

Per updated requirements: for a doctor's 09:00-12:00 window, the 9:00-9:15 slot stays bookable even after 9:15 has passed - up until the whole window (12:00) ends, not just that slot's own narrow end time. Changed in `SlotService.findWindowEndTime` (new): given a generated slot, it looks up which active `DoctorAvailability` template that slot falls entirely within, and uses **that template's end time** as the cutoff instead of the slot's own `endTime`. Both `getOrGenerateSlots` (the listing) and `AppointmentService.lockAndValidateSlot` (the actual gate at booking time, via the new public `SlotService.resolveBookableCutoff`) now use this same window-level cutoff, so they can't drift apart.

Falls back to the slot's own end time if no active template matches (e.g. the template was edited/deactivated after the slot was generated - `resyncFutureSlots` normally prevents this for `AVAILABLE` slots already, so this is a safety net rather than the expected path).
