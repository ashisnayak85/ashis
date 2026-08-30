# OneHealth Clinic Platform - Frontend

React (Vite) frontend for the OneHealth backend (`onehealth-clinic-platform-backend.zip`).
Same stack/conventions as the doctor-appointment-marketplace frontend it was
forked from (React 19, react-router 7, axios with silent-refresh interceptor),
plus [Recharts](https://recharts.org) for the owner's analytics dashboard.

## Setup

```
npm install
npm run dev
```

Runs on `http://localhost:5175` (matches the backend's CORS allow-list in
`application.properties`). Talks to the backend at the URL in `.env`
(`VITE_API_BASE_URL`, defaults to `http://localhost:8083/api`).

`VITE_ORG_SLUG` in `.env` is the organization patients register under - for a
single-customer deployment this is fixed to that customer's slug (`one-health`
by default) so patients never have to know or type it.

## Logging in

Use the accounts seeded by the backend's `DataSeeder` (see its README):
- `OWNER` - the demo org's owner login (`app.seed.owner-email` / `app.seed.owner-password`)
- Create `CLINIC_ADMIN` and `DOCTOR` logins from the Owner portal (Branches / Doctors pages) once you've logged in as the owner.
- Patients self-register from `/register`.

## Structure

```
src/
  api/            one file per role, thin wrappers around axios calls
  components/     shared shell: Navbar, DashboardLayout (sidebar), ProtectedRoute
  context/        AuthContext (session), SidebarContext (dashboard sidebar open state)
  pages/          public pages (Home, Login, Register, BookAppointment, MyAppointments)
    owner/        Dashboard (charts), Branches, Doctors
    clinic-admin/ Dashboard, Book walk-in, Appointments
    doctor/       Appointments, Availability
```

## Owner dashboard

`pages/owner/OwnerDashboard.jsx` is the main deliverable: date-range filter
(defaults to today, with Today/7-day/30-day presets), an optional branch
filter, and four charts pulled straight from `GET /api/owner/dashboard`:

- **Location-wise completion rate** (bar) - the "% patients came and got service" view, per branch
- **Appointments by branch** (stacked bar) - online vs. walk-in volume per branch
- **Overall outcome split** (pie) - completed / no-show / cancelled / still-booked
- **Appointments over time** (line) - daily trend across the selected range

Plus full data tables underneath (branch breakdown, doctor utilization) for
anything the charts summarize but don't show every number for. If a section
fails to compute on the backend, it now renders empty rather than taking down
the whole dashboard (see backend README's "Dashboard resilience" note) -
watch for `sectionWarnings` in the API response if you want to surface those
notices in the UI (not yet wired into the chart cards themselves).

## Update: new pages from testing feedback

- **`components/ClinicLocationPicker.jsx`** - address search (OpenStreetMap
  Nominatim, free, no API key) + an interactive Leaflet map with a
  click/drag-to-place marker, used on the "Add a new branch" form. Fills
  `latitude`/`longitude` plus a best-effort address/city/pincode from the
  geocoder, which can still be hand-edited afterward.
- **`pages/owner/OwnerSpecializations.jsx`** - add/rename/activate/deactivate/
  delete the org's specialization master list.
- **`pages/owner/OwnerDoctors.jsx`** - doctor specializations are now a
  multi-select chip picker sourced from that master list (was free text).
- **`pages/owner/OwnerEmployees.jsx`** + **`OwnerEmployeeDetail.jsx`** - staff
  roster (clinic authorities + doctors, both treated as employees) with HR
  profile fields (gender, DOB, joining date, addresses) and an append-only
  salary history (new revisions are added, never overwrite a previous entry).
  Owner-only throughout - salary is never exposed on any doctor-facing DTO.

## Known simplifications (see backend README for the full list)

- No ratings/reviews UI (dropped from this scaffold along with the backend entity - see backend README).
- No payment collection UI - `paymentStatus` exists but nothing sets it to `PAID` yet.
- Nominatim (the free geocoder behind the location picker) asks for roughly
  1 request/second and no heavy automated use - fine for occasionally adding a
  branch, but don't wire it into a bulk-import script without switching to a
  paid provider or self-hosted instance first.

## Update: map search gap + doctor/branch ID pickers

**Non-popular addresses not found in map search.** Nominatim (the free OpenStreetMap geocoder) is built from crowd-sourced data, and its coverage of small/less-traveled addresses is genuinely weaker than Google's - that's not a bug, it's the trade-off of avoiding a Google Cloud billing account. `ClinicLocationPicker.jsx` now handles this: if the built-in search comes back empty, a "Find on Google Maps" button opens a real Google Maps search (their free public website, not the metered API - no billing needed) in a new tab. The person finds the exact spot there, right-clicks it to copy the coordinates Google shows in the context menu, and pastes them back into a box on the form - `parseLatLngFromGoogleMapsText()` reads either a raw `"lat, lng"` pair or a full Google Maps URL (several of Google's URL coordinate formats are handled; shortened `maps.app.goo.gl` share links are not, since their coordinates aren't visible without following a server-side redirect - the UI tells people to use the right-click method or the full address-bar URL instead).

**"How would a doctor/front-desk person know a numeric ID?"** Two forms asked for a raw ID with no way to look one up:
- Walk-in booking (`ClinicAdminWalkIn.jsx`) asked for a doctor ID - now a name dropdown, backed by `GET /api/clinic-admin/doctors`.
- Doctor availability (`DoctorAvailability.jsx`) asked for a branch ID - now a name dropdown, backed by `GET /api/doctor/my-clinics`.
