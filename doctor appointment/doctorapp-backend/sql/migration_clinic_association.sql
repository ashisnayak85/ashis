-- =============================================================================
-- Migration: many-to-many Doctor <-> Clinic via ClinicAdmin ownership
-- =============================================================================
-- Context: `clinics` used to have a direct `doctor_id` FK (one doctor "owns" a
-- clinic). This migration introduces:
--   1. clinic_admins            - a new owner type for clinics (their own login)
--   2. doctor_clinic_associations - the real many-to-many join, with an
--                                   invite/request + approve workflow
-- and repoints `clinics` at clinic_admins instead of doctors.
--
-- Run this ONCE against an existing database that already has data from the
-- old schema. If you're still in early development and don't care about
-- existing rows, it's simpler to just drop the database and let
-- `spring.jpa.hibernate.ddl-auto=update` recreate everything fresh - in that
-- case you don't need this file at all.
--
-- This script is written for MySQL (matches application.properties).
-- =============================================================================

USE doctorapp_db;

-- -----------------------------------------------------------------------------
-- 1. New tables
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS clinic_admins (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    created_at  DATETIME NOT NULL,
    CONSTRAINT uk_clinic_admins_user UNIQUE (user_id),
    CONSTRAINT fk_clinic_admins_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS doctor_clinic_associations (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id      BIGINT NOT NULL,
    clinic_id      BIGINT NOT NULL,
    initiated_by   VARCHAR(10) NOT NULL,   -- DOCTOR | CLINIC
    status         VARCHAR(10) NOT NULL,   -- PENDING | APPROVED | REJECTED | REMOVED
    created_at     DATETIME NOT NULL,
    responded_at   DATETIME NULL,
    CONSTRAINT uk_doctor_clinic UNIQUE (doctor_id, clinic_id),
    CONSTRAINT fk_dca_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_dca_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 2. Add new columns to clinics (nullable for now - we backfill next, then
--    tighten to NOT NULL and drop the old doctor_id column at the end)
-- -----------------------------------------------------------------------------

ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS clinic_admin_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active   TINYINT(1) NOT NULL DEFAULT 1;

-- -----------------------------------------------------------------------------
-- 3. Backfill: give every legacy (doctor-owned) clinic a placeholder
--    clinic-admin account, so existing data still satisfies the new NOT NULL
--    FK once we tighten it below.
--
--    These placeholder accounts are created disabled (enabled = 0) with a
--    random unusable password hash, since nobody should log in as them - they
--    exist purely so the FK is satisfiable. A real clinic admin can be
--    assigned to "claim" the clinic later via a future admin/support flow;
--    that UI isn't built as part of this change.
-- -----------------------------------------------------------------------------

INSERT INTO users (email, password, role, enabled, created_at)
SELECT
    CONCAT('legacy-clinic-', c.id, '@migration.local'),
    '$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvalidin', -- unusable bcrypt-shaped hash
    'CLINIC_ADMIN',
    0,
    NOW()
FROM clinics c
WHERE c.clinic_admin_id IS NULL
  AND c.doctor_id IS NOT NULL;

INSERT INTO clinic_admins (user_id, name, phone, created_at)
SELECT
    u.id,
    CONCAT('Unclaimed clinic admin (clinic #', c.id, ')'),
    NULL,
    NOW()
FROM clinics c
JOIN users u ON u.email = CONCAT('legacy-clinic-', c.id, '@migration.local')
WHERE c.clinic_admin_id IS NULL
  AND c.doctor_id IS NOT NULL;

UPDATE clinics c
JOIN users u ON u.email = CONCAT('legacy-clinic-', c.id, '@migration.local')
JOIN clinic_admins ca ON ca.user_id = u.id
SET c.clinic_admin_id = ca.id
WHERE c.clinic_admin_id IS NULL
  AND c.doctor_id IS NOT NULL;

-- Legacy clinics were already effectively "live" in the app, so mark them
-- verified/active rather than dropping them out of search on migration day.
UPDATE clinics SET verified = 1, active = 1 WHERE doctor_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 4. Backfill: turn every legacy clinics.doctor_id row into an APPROVED
--    association, so existing doctor <-> clinic pairs keep working exactly as
--    before (their availability/slots/appointments are untouched).
-- -----------------------------------------------------------------------------

INSERT INTO doctor_clinic_associations (doctor_id, clinic_id, initiated_by, status, created_at, responded_at)
SELECT
    c.doctor_id,
    c.id,
    'CLINIC',
    'APPROVED',
    NOW(),
    NOW()
FROM clinics c
WHERE c.doctor_id IS NOT NULL
ON DUPLICATE KEY UPDATE status = 'APPROVED';

-- -----------------------------------------------------------------------------
-- 5. Tighten constraints now that every row has a clinic_admin_id, then drop
--    the old doctor ownership column and its FK.
-- -----------------------------------------------------------------------------

-- Find and drop the old FK by name if you don't know it, run:
--   SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
--   WHERE TABLE_SCHEMA = 'doctorapp_db' AND TABLE_NAME = 'clinics' AND COLUMN_NAME = 'doctor_id';
-- then replace fk_clinics_doctor below with the actual name if different.

ALTER TABLE clinics
    MODIFY COLUMN clinic_admin_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_clinics_clinic_admin FOREIGN KEY (clinic_admin_id) REFERENCES clinic_admins(id);

-- Drop the legacy doctor_id column + its FK/index (name may differ - check first).
ALTER TABLE clinics DROP FOREIGN KEY fk_clinics_doctor;
ALTER TABLE clinics DROP COLUMN doctor_id;

-- -----------------------------------------------------------------------------
-- Done. From here on, spring.jpa.hibernate.ddl-auto=update can keep managing
-- the schema day-to-day, since it now matches the new entity model.
-- =============================================================================
