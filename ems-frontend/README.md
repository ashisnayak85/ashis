# EMS Frontend (React)

React SPA for the Enterprise Employee Management System, replacing the Thymeleaf
templates in `src/main/resources/templates`. Talks to the existing Spring Boot
REST APIs over HTTP with session-cookie auth (no rewrite to JWT needed).

## Backend changes required

These are already applied if you're using the updated backend zip provided
alongside this app. If you're wiring this into your own copy of the backend,
apply `SecurityConfig.java` and add `AuthApiController.java` as described
below - the frontend will not work without them:

1. **CORS** - a `CorsConfigurationSource` bean allowing `http://localhost:5173`
   with `allowCredentials(true)`.
2. **JSON login/logout responses** - `formLogin` and `logout` need custom
   success/failure handlers that return JSON, not the default HTML redirects
   (React can't follow a `defaultSuccessUrl` redirect meaningfully).
3. **Cookie-based CSRF** - `CookieCsrfTokenRepository.withHttpOnlyFalse()` so
   the frontend can read the `XSRF-TOKEN` cookie and echo it back as the
   `X-XSRF-TOKEN` header (this app's `src/api/client.js` already does this).
4. **`GET /api/auth/me`** - new endpoint returning the current session's
   username/roles, or 401 if not authenticated. Used on app load to restore
   session state.

## Running locally

```bash
npm install
npm run dev
```

The dev server runs at `http://localhost:5173` by default and expects the
Spring Boot backend at `http://localhost:8080` (see `.env` -
`VITE_API_BASE_URL`). Start the backend first (`mvn spring-boot:run`), then
the frontend.

## Building for production

```bash
npm run build
```

Outputs to `dist/`. Serve this with any static host (Nginx, Vercel, S3+CloudFront,
etc). Update `VITE_API_BASE_URL` and the CORS allowed origin in
`SecurityConfig.java` to match wherever `dist/` ends up being served from.

## Structure

```
src/
  api/            axios calls, one file per backend resource
  context/        AuthContext - session state, login/logout, role checks
  components/     shared UI (Layout/nav, Pagination, modals, ProtectedRoute)
  pages/          one component per route
```

## Pages implemented

| Route | Old Thymeleaf template | Notes |
|---|---|---|
| `/login` | `auth/login.html` | posts to Spring's `/login` |
| `/dashboard` | `dashboard/index.html` | stats from `/api/dashboard/stats` |
| `/employees` | `employee/list.html` | active employees, full CRUD |
| `/all-employees` | `employee/allEmployeeList.html` | every row (active + inactive), edit only |
| `/departments` | `department/list.html` | CRUD, gated by ADMIN/MANAGER |
| `/attendance` | `attendance/list.html` | mark + view by employee |
| `/leaves` | `leave/list.html` | apply; approve/reject gated by ADMIN/MANAGER |
| `/admin/users` | `user/list.html` | ADMIN only |
| `/profile` | `profile/index.html` | current session info |
