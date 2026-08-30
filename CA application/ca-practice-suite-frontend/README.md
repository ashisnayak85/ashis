# CA Practice Suite — Frontend

React + Vite SPA for the CA Practice Suite backend, structured the same way
as the reference EMS frontend: `api/` (one file per resource, axios-based),
`context/AuthContext.jsx` (session state), `components/` (shared UI),
`pages/` (one per screen), `App.jsx` (router).

## Running it

```bash
cd ca-practice-suite-frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` by default and talks to the backend at the
URL in `.env` (`VITE_API_BASE_URL`, defaults to `http://localhost:8080`).
Make sure the backend is running first — see the backend README — then log
in with `admin` / `Admin@123`.

## Pages

- **Login** — session login against Spring Security.
- **Dashboard** — YTD income/expense, GST payable, overdue compliance,
  unpaid invoices, upcoming deadlines, recent ledger activity.
- **Clients** — onboard/edit/deactivate clients; search by name/type.
- **Ledger** — post entries, filter by client/type/date/reconciled, toggle
  reconciliation, export to `.xlsx`.
- **Invoices** — create draft invoices, move through
  DRAFT → SENT/PAID/OVERDUE → CANCELLED.
- **Compliance** — schedule GST/TDS/ROC deadlines, mark filed (auto-creates
  the next recurrence for recurring task types), filter by status/type/client.
- **Chart of Accounts**, **Users** — ADMIN only.
- **Profile** — change password.

## Notes

- CSRF: the backend hands out an `XSRF-TOKEN` cookie; `api/client.js` reads
  it and echoes it back as `X-XSRF-TOKEN` on every mutating request — no
  manual wiring needed per page.
- All list endpoints are paginated; every page follows the same
  `search → data.content / data.page / data.totalPages` shape via
  `<Pagination />`.
