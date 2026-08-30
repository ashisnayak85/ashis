# CA Practice Suite — Backend

A client-ledger, GST-ready invoicing, and compliance-deadline engine for a
Chartered Accountant's practice (or an SME finance team), built with the same
layered Spring Boot architecture as the reference EMS project:
`entity -> repository (+ Specifications) -> mapper -> dto -> service/impl ->
REST controller`, a shared `ApiResponse<T>` envelope, a `GlobalExceptionHandler`,
and session-based Spring Security for a React SPA.

## What it does

- **Clients** — every business/individual the firm manages books for (GSTIN,
  PAN, address, active/inactive).
- **Chart of Accounts** — the ledger heads (Sales Revenue, Office Rent, GST
  Payable, etc.) everything posts against.
- **Ledger** — the general ledger. Post entries directly, or let an invoice
  auto-post one. GST rate/amount is computed server-side so it's never wrong
  because a form field was typed differently. Entries can be marked
  "reconciled" (matched against the bank statement / GSTR-2B).
- **Invoices** — sales & purchase invoices with a DRAFT → SENT/PAID →
  (optionally CANCELLED) workflow. Moving to SENT or PAID auto-posts exactly
  one matching ledger entry — the books never drift from the invoice register.
- **Compliance calendar** — GSTR-1/3B, TDS returns, ROC filings, etc., each
  with a due date, status, and recurrence. Filing a MONTHLY/QUARTERLY/etc.
  task automatically schedules the next period's task. A daily scheduled job
  flips anything past its due date to OVERDUE.
- **Dashboard** — YTD income/expense, net GST payable (output − input credit),
  overdue compliance count, unpaid invoice total, recent activity.

## Running it

Requires JDK 21 and Maven.

```bash
cd ca-practice-suite
mvn spring-boot:run
```

By default it runs on the `dev` profile against **local MySQL** — no schema
to write by hand, `createDatabaseIfNotExist=true` creates the
`ca_practice_suite` database and Hibernate creates all the tables from the
entity classes on first boot. Seed data (roles, an admin login, a standard
chart of accounts, and one sample client) is created automatically too.

Requirements: MySQL running locally. Defaults assume a fresh install
(`root` user, no password) — override if yours differs:

```bash
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
mvn spring-boot:run
```

**Default login:** `admin` / `Admin@123` — change this immediately in a real
deployment (`PUT /api/admin/users/{id}` for management, or the change-password
endpoint for the logged-in user).

Once it's running, inspect the data with whatever MySQL client you already
use (MySQL Workbench, DBeaver, TablePlus, `mysql` CLI, etc.) against the
`ca_practice_suite` database on `localhost:3306`.

### No MySQL installed? Use the H2 profile instead

Zero-install, in-memory database — good for a quick look, but **all data is
lost on every restart**:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Browse its tables at `http://localhost:8080/h2-console` (JDBC URL
`jdbc:h2:mem:ca_practice`, user `sa`, blank password) — but for real
day-to-day development, prefer the MySQL path above.

### Real deployment (MySQL, different credentials/host per environment)

```bash
export DB_URL=jdbc:mysql://your-host:3306/ca_practice_suite?createDatabaseIfNotExist=true
export DB_USERNAME=produser
export DB_PASSWORD=yourpassword
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## API shape

Every endpoint returns `{ success, message, data, errors }`. List endpoints
support `page`/`size` and return a `PageResponse<T>`. Auth is a standard
Spring Security session (`POST /login` with form-encoded `username`/`password`,
`POST /logout`, `GET /api/auth/me`) with CSRF handed out as a readable
`XSRF-TOKEN` cookie for the SPA to echo back as `X-XSRF-TOKEN`.

Key routes: `/api/clients`, `/api/accounts`, `/api/ledger`,
`/api/ledger/export` (xlsx), `/api/invoices`, `/api/compliance`,
`/api/compliance/upcoming`, `/api/dashboard/stats`, `/api/admin/users`
(ADMIN only).

## What's deliberately out of scope for this MVP

This is a wedge, not the whole practice-management suite (see the product
plan this came from): no direct GSTN/e-invoicing portal integration, no
multi-currency, no line-item-level invoices, no document/file attachments,
no client self-service portal. Those are the natural next slices once this
core loop (client → ledger → invoice → compliance → dashboard) is validated
with real users.
