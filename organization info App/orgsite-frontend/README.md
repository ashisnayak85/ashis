# OrgSite Frontend

React + Vite frontend for the OrgSite platform: a public marketing page per
business, plus a simple admin panel for owners to manage their own content.

## Quick start

```bash
npm install
npm run dev
```

Runs on **http://localhost:5175** (matches the backend's CORS allow-list — see
`app.frontend-url` in the backend's `application.properties`). Make sure the
backend is running first at `http://localhost:8083` (see `orgsite-backend/README.md`).

`.env` already points at the backend:
```
VITE_API_BASE_URL=http://localhost:8083/api
```

## Pages

- `/` — landing page for the platform itself
- `/register`, `/login` — owner signup/login
- `/admin` — business profile editor (logo, cover photo, contact info, hours, theme color, publish toggle)
- `/admin/content` — manage menu items / services, gallery photos, testimonials, announcements
- `/:slug` — the public page anyone can visit, e.g. `/sunrise-tea-house` (seeded demo org)

## Build for production

```bash
npm run build
```

Outputs static files to `dist/` — deploy that folder to any static host
(Vercel, Netlify, S3+CloudFront, nginx, etc). Remember to point
`VITE_API_BASE_URL` at your deployed backend before building.

## Notes

- Auth tokens are stored in `localStorage` (`accessToken`, `refreshToken`) —
  standard for a real deployed app (this restriction only applies inside
  Claude's in-chat Artifacts preview, not to a project you run yourself).
- `src/api/client.js` auto-attaches the JWT to every request and silently
  refreshes it once on a 401 before giving up and redirecting to `/login`.
- Uploaded image URLs come back from the backend as relative paths
  (e.g. `/uploads/org-3/abc.jpg`); `src/api/config.js` resolves them against
  the backend's origin so `<img>` tags work without extra wiring.
