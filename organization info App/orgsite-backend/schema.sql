-- =============================================================================
-- OrgSite Platform - MySQL schema
-- Run this once against your MySQL server (e.g. via MySQL Workbench, phpMyAdmin,
-- or `mysql -u root -p < schema.sql`). It creates the database and all three
-- tables the backend needs. After this, the app talks straight to these tables -
-- Hibernate will NOT auto-alter them (see application.properties changes).
-- =============================================================================

CREATE DATABASE IF NOT EXISTS orgsite_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE orgsite_db;

-- -----------------------------------------------------------------------------
-- organizations: one row per business (tea shop, restaurant, school, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150)  NOT NULL,
    slug              VARCHAR(80)   NOT NULL,
    category          VARCHAR(30)   NOT NULL,
    tagline           VARCHAR(200),
    description       VARCHAR(2000),
    address           VARCHAR(300),
    phone             VARCHAR(30),
    whatsapp          VARCHAR(30),
    email             VARCHAR(150),
    map_embed_url     VARCHAR(500),
    facebook_url      VARCHAR(500),
    instagram_url     VARCHAR(500),
    website_url       VARCHAR(500),
    logo_url          VARCHAR(500),
    cover_image_url   VARCHAR(500),
    theme_color       VARCHAR(20)   DEFAULT '#2563eb',
    hours_text        VARCHAR(500),
    published         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        DATETIME      NOT NULL,
    UNIQUE KEY uq_organizations_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- users: one row per business owner/admin account, linked to one organization
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(150)  NOT NULL,
    password          VARCHAR(255)  NOT NULL,
    role              VARCHAR(20)   NOT NULL,
    organization_id   BIGINT,
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        DATETIME      NOT NULL,
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- content_blocks: flexible content items (gallery photo / menu item /
-- testimonial / announcement) belonging to one organization
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_blocks (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id   BIGINT        NOT NULL,
    type              VARCHAR(30)   NOT NULL,
    title             VARCHAR(150),
    subtitle          VARCHAR(500),
    description       VARCHAR(2000),
    image_url         VARCHAR(500),
    price             VARCHAR(30),
    sort_order        INT           NOT NULL DEFAULT 0,
    visible           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        DATETIME      NOT NULL,
    CONSTRAINT fk_content_blocks_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE,
    INDEX idx_content_blocks_org_sort (organization_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Done. The backend's DemoDataSeeder will insert one sample organization
-- ("Sunrise Tea House") the first time it starts against these empty tables.
