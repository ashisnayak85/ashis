# OrgSite Backend

Multi-tenant marketing/brochure website platform. Each business (tea shop,
restaurant, school, shop, etc.) is one "Organization" and gets a public page
at `/{slug}`. Business owners log in to a simple admin panel to manage their
own content — no coding required on their end.

## Stack
Spring Boot 3, Java 17, Spring Security + JWT, Spring Data JPA, H2 (default) / MySQL.

## Quick start

1. Run `schema.sql` once against your MySQL server to create the database and tables:
   ```bash
   mysql -u root -p < schema.sql
   ```
   (Or paste its contents into MySQL Workbench / phpMyAdmin and run it.)

2. Open `src/main/resources/application.properties` and set your real MySQL
   username/password if they're not the default `root` / `root`.

3. Start the app:
   ```bash
   mvn spring-boot:run
   ```

The app boots on **http://localhost:8083** and seeds one demo organization the
first time it runs against the empty tables:

- Public page: `http://localhost:8083/api/public/org/sunrise-tea-house`
  (view it properly through the frontend at `http://localhost:5175/sunrise-tea-house`)
- Demo owner login: `owner@sunriseteahouse.example` / `Demo@1234`

## Schema is fully manual now

`spring.jpa.hibernate.ddl-auto=validate` — Hibernate checks your entities match
what's in the database on startup and fails loudly if they don't, but it will
**never** create or alter a table itself. `schema.sql` is the single source of
truth for your database structure. If you ever add a new field to an entity,
add the matching `ALTER TABLE` to `schema.sql` (or a new migration file) and
run it yourself before restarting the app — the two now have to be kept in
sync by hand, which is the trade-off for full control over your schema.

Want to go back to zero-setup local testing without MySQL? Comment out the
MySQL block in `application.properties` and uncomment the H2 block below it
(and switch `ddl-auto` back to `update` for that case only — H2 is throwaway
anyway).

## How multi-tenancy works

- `Organization` = one business. Has a unique `slug` used in the public URL.
- `User` = an OWNER account, always linked to exactly one `Organization`.
- `ContentBlock` = one flexible content item (gallery photo / menu item /
  testimonial / announcement) belonging to one organization.
- Every `/api/admin/**` endpoint derives the organization id from the JWT
  (`UserPrincipal.getOrganizationId()`), never from the URL or request body —
  this is what stops one owner editing another business's page.
- `/api/public/**` is fully open, no login, and only returns organizations
  where `published = true`.

## Key endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create a new org + owner account in one call |
| POST | `/api/auth/login` | none | Log in |
| POST | `/api/auth/refresh` | none | Refresh an access token |
| GET | `/api/public/org/{slug}` | none | Everything needed to render one public page |
| GET/PUT | `/api/admin/organization` | OWNER | View/edit your own business profile |
| PATCH | `/api/admin/organization/publish?published=true` | OWNER | Publish/unpublish |
| GET/POST | `/api/admin/content-blocks` | OWNER | List/create content items |
| PUT/DELETE | `/api/admin/content-blocks/{id}` | OWNER | Edit/delete a content item |
| POST | `/api/admin/upload` | OWNER | Upload an image (multipart `file` field), returns a URL |

## Adding a new business category or content type

Both are plain enums, edit and redeploy:
- `Organization.Category` in `entity/Organization.java`
- `ContentBlock.BlockType` in `entity/ContentBlock.java`

## Production notes before going live

- Change `app.jwt.secret` (set the `JWT_SECRET` env var) — the default is a
  placeholder and must not ship to production.
- Uploaded images are stored on local disk under `uploads/`. For real
  deployment, swap `FileStorageService` to write to S3/Cloudinary/similar so
  images survive redeploys and scale across multiple server instances.
- Set `app.frontend-url` to your real frontend domain for CORS.
