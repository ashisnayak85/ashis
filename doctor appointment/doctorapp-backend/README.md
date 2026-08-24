# Nearby Doctor Appointment Platform — Backend (MVP)

Standalone Spring Boot service. Independent from the EMS project: own `pom.xml`,
own port (`8082`), own database (`doctorapp_db`), own JWT-based stateless auth.

## What's implemented (Phase 1 from the product plan)

- Patient & doctor registration/login (JWT access + refresh tokens)
- Doctor profile + clinic(s) with lat/lng
- **Nearby search**: Haversine distance query, bounding-box pre-filtered, optional
  specialization filter, radius in km
- Doctor weekly availability template → on-demand generation of real bookable
  `AppointmentSlot` rows for a given date
- Transaction-safe booking (pessimistic row lock + DB unique constraint — two
  patients can never book the same slot)
- Cancel appointment (frees the slot back to AVAILABLE)
- Patient's appointment history

**Deliberately not built yet** (see product roadmap Phase 2/3 — payments,
reviews, notifications, video consultation, admin dashboard). Doctor
verification currently has no admin UI; verify a doctor manually for testing:

```sql
UPDATE doctors SET verified = true WHERE id = 1;
```

## Run it

1. Create a MySQL 8 instance (or let `createDatabaseIfNotExist=true` do it).
2. `cp src/main/resources/application.properties` and adjust DB credentials,
   or export `JWT_SECRET` as an env var for anything beyond local dev.
3. `mvn spring-boot:run` (needs Maven + a network path to Maven Central — this
   sandbox couldn't verify a full build because outbound access to
   `repo.maven.apache.org` isn't on the allowed domain list here; please run
   `mvn clean install` locally to confirm compilation).
4. API is live at `http://localhost:8082`.

## Key endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register/patient` | public | Patient signup |
| POST | `/api/auth/register/doctor` | public | Doctor signup (starts unverified) |
| POST | `/api/auth/login` | public | Get access + refresh tokens |
| POST | `/api/auth/refresh` | public | Exchange refresh token for a new access token |
| GET | `/api/doctors/nearby?lat=&lng=&radiusKm=&specialization=` | public | Core search |
| GET | `/api/doctors/{id}/profile` | public | Doctor profile + clinics |
| GET | `/api/doctors/{id}/slots?clinicId=&date=` | public | Available slots for a date |
| POST | `/api/doctor/availability` | DOCTOR | Add a weekly working-hours template |
| POST | `/api/doctors/me/clinics` | DOCTOR | Add a clinic |
| POST | `/api/patient/appointments` | PATIENT | Book a slot (`{ "slotId": 1 }`) |
| GET | `/api/patient/appointments` | PATIENT | My appointment history |
| PUT | `/api/patient/appointments/{id}/cancel` | PATIENT | Cancel a booking |

Send the JWT as `Authorization: Bearer <accessToken>`.

## Why JWT instead of the EMS's session+CSRF pattern

This app is public-facing and phone-first — patients expect to stay logged in
for a while, and a stateless token fits a future mobile app better than a
server session ever would. It's a deliberate difference from the EMS project,
not an inconsistency.
