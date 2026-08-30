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

## Not included in this scaffold (intentionally, to keep the build focused)

- Doctor ratings/reviews (present in the marketplace project; drop back in if the owner wants it - straightforward to port).
- Payment gateway integration (`PaymentStatus` enum is there; wiring an actual gateway is a follow-up).
- Self-serve org signup / billing for a second customer (only matters once you actually have a second paying customer - see the multi-tenancy discussion in the project chat history).
- Frontend (this deliverable is backend-only; ask if you'd like the equivalent React scaffold - owner dashboard with charts, walk-in booking screen, date-range filter - next).
