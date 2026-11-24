# Hiring Module

Takes a vacancy from **approved requisition -> candidate pipeline -> interviews -> approved offer -> new Employee**.
Candidates are a separate domain: they never appear in the `employee` table until an accepted offer is converted
through the existing `EmployeeService.create()`.

## 1. First-time setup

1. **Database**
   - dev (`ddl-auto=update`): tables are created automatically on startup.
   - prod (`ddl-auto=validate`): run `src/main/resources/db/hiring_schema.sql` once, after `schema.sql` and `resignation_schema.sql`.
2. **Config** (`application.properties`, already added):
   - `app.hiring.storage-dir=hiring-files` - private resume folder. Back it up like the DB. Do **not** point it at `app.upload.dir`.
   - `app.hiring.company-name=...` - **change this**; it appears in offer letters and candidate e-mails.
3. **Start the backend.** On startup it creates the workflow modules `REQUISITION` and `OFFER` and the business role `Recruiter`.
4. **Give HR people the Recruiter role:** admin screen *Employee Access* -> assign the business role **Recruiter** (no department scope needed).
   Admins already have full hiring access and do not need it.
5. **Configure approvals** in *Workflow Setup* for module **Job Requisition** and **Job Offer** (e.g. Reporting Manager -> HR Head).
   - Requisition chain is relative to the person who raised it; the offer chain is relative to the requisition's **hiring manager**.
   - If no stage is configured (or none resolves to a person), the item is **auto-approved** - it never gets stuck. Configure stages if you want a real approval.
   - Nobody approves their own item: if the resolved approver is the requester (requisition) or the offer's drafter (offer), that stage is skipped.

## 2. Who can do what

| Person | Can |
|---|---|
| **Admin / Recruiter** ("hiring staff") | Everything: all requisitions, candidates (incl. salary expectations), pipeline, interviews, offers, convert to employee. Dashboard. |
| **Manager** (auth role MANAGER) | Raise requisitions; see requisitions they raised/manage; work the pipeline and see candidates of *their* requisitions (no salary figures). |
| **Approver** (named by the workflow) | See and approve/reject the requisition/offer waiting on them (Hiring Approvals). |
| **Interviewer** (on a panel) | See their interviews, the candidate profile + resume, submit feedback. Other panelists' feedback is hidden until they submit their own (blind feedback). |
| **Anyone else** | Nothing (403). |
| **Admin only** | Anonymise a candidate (data-retention / deletion request). |

## 3. Flow

```
Requisition: DRAFT -> PENDING_APPROVAL -> APPROVED -> OPEN -> (ON_HOLD) -> CLOSED      (REJECTED -> edit -> DRAFT)
Application: APPLIED -> SCREENING -> INTERVIEW -> OFFER -> HIRED      (+ REJECTED / WITHDRAWN; REJECTED can be reopened to SCREENING)
Offer:       DRAFT -> PENDING_APPROVAL -> APPROVED -> SENT -> ACCEPTED -> CONVERTED   (+ DECLINED / EXPIRED / REVOKED)
```
- OFFER stage is entered by *creating an offer* (needs >= 1 completed interview); HIRED only by *converting* an accepted offer.
- Revoked/expired offer -> candidate returns to INTERVIEW (revised offer possible). Declined -> application WITHDRAWN.
- Offer above the requisition's max budget needs a written exception reason.
- When all openings are filled the requisition closes automatically.
- Sent offers past their expiry date become EXPIRED (nightly job + checked on read).

## 4. Try it end to end (about 10 minutes)

1. Admin: *Employee Access* -> give yourself or a colleague **Recruiter**. *Workflow Setup* -> add one stage to **Job Requisition** and one to **Job Offer** (or skip to auto-approve).
2. *Hiring > Requisitions* -> **New requisition** -> open it -> **Submit for approval** -> approver approves under *Hiring Approvals*.
3. As recruiter: **Open for hiring**.
4. *Candidates* -> **New candidate** (tick consent, attach a PDF) -> on the candidate page add to the requisition.
5. Open the application -> **Move** to Screening -> **Schedule interview** (pick a panel) -> each interviewer: *My Interviews* -> **Give feedback**.
6. Application -> **Draft offer** -> **Submit for approval** -> approve -> **Send to candidate** -> **Candidate accepted** -> **Convert to employee**.

## 5. API summary

All under `/api`, session + CSRF like the rest of the app; JSON in `ApiResponse` envelope.
`requisitions` (create, update, `/{id}/submit|approve|reject|status|assign-recruiter`, `/search`, `/pending-approvals`) ·
`candidates` (CRUD-ish, `/search`, `/{id}/resume` POST upload / GET download, `/{id}/anonymise`) ·
`applications` (apply, `/by-requisition/{id}`, `/by-candidate/{id}`, `/{id}/move|history`) ·
`interviews` (`/by-application/{id}`, `/my`, `/{id}` PUT, `/{id}/cancel|status|feedback`) ·
`offers` (`/by-application/{id}`, `/search`, `/pending-approvals`, `/{id}/submit|approve|reject|send|accept|decline|revoke|convert|letter`) ·
`hiring` (`/me`, `/dashboard`, `/lookups`, `/employees?keyword=`).

## 6. Decisions you should know about

- **Resumes are private.** They are stored in `app.hiring.storage-dir` (not served statically), only PDF/DOC/DOCX with a real
  file-signature check, random server-side names, and only downloadable through an endpoint that checks access and writes an audit entry.
  I deliberately did **not** reuse the existing file upload (see section 8).
- **No Flyway yet.** I could not test a migration baseline against your real database, so I followed your existing
  convention (entities + a reviewable `hiring_schema.sql`). Adopting Flyway is still recommended.
- **Enum columns are plain VARCHAR** (`@JdbcTypeCode(VARCHAR)`), not MySQL ENUM, so adding a status later never needs an ALTER TABLE.
- **Offer letter is printable HTML** (open the letter, then Print -> Save as PDF), not a generated PDF - no new library, no new dependency.
- **Convert to employee:** employee code is generated (`EMP001`, ...) unless typed; `Employee.salary` defaults to **annual CTC / 12**
  - this is an assumption, the convert dialog lets you change it to match your payroll convention.
- **Rehire is blocked, not automated.** If the candidate's email already exists as an Employee, conversion stops with a clear message.
- **A user login is not auto-created** at conversion; create it under Users as you do today.
- Stages are fixed in v1 (not configurable per requisition). No public careers page yet.
- `AccessDeniedException` now returns **403** (previously any service-level denial would have surfaced as 500) and optimistic-lock
  conflicts return **409** with a friendly message - both via `GlobalExceptionHandler`.

## 7. Privacy

Candidate consent is required when creating a candidate and timestamped. Salary figures are hidden from non-staff.
Admin can anonymise a candidate (wipes personal data + resume, keeps history). Confirm retention periods and DPDP obligations with your
compliance contact - this implements the mechanism, not the policy.

## 8. Existing issues I found but did not change (out of scope, worth fixing)

- `/uploads/**` is `permitAll` and statically served: anything in `app.upload.dir` (profile photos, existing uploads) is downloadable by URL.
- `/api/files/{id}/download` only requires login - any signed-in user can download any file by id.
- `FileStorageServiceImpl` keeps the client's file extension with no content check.
- DB credentials (`root/root`) are in `application-dev.properties`; use environment variables.
- Employee creation requires `employeeCode` from the client elsewhere; the new generator is only used by hiring.

## 9. Verification status (be aware)

Verified: all Java parses with the JDK compiler (no syntax errors); every repository method, getter, builder call and signature used by the new
code was cross-checked against your actual classes; the pipeline state machine and its unit test were executed; the frontend lints (0 errors in
new files) and production-builds. **Not verified:** the backend was not compiled with Maven/Spring/Lombok and nothing was run against MySQL
(those dependencies were not reachable where this was built). Run `mvn clean verify` first; fix-ups, if any, should be small (imports/typos).
Also not covered by automated tests: services, access rules, blind feedback, offer conversion - these deserve tests next.
