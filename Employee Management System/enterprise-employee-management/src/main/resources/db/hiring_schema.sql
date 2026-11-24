-- =============================================================================
-- EEMS Database Schema - Hiring module (requisitions, candidates, pipeline,
-- interviews, offers).
-- Run this AFTER schema.sql and resignation_schema.sql (adds to the existing eems_db).
-- Same convention as resignation_schema.sql: a clean, reviewable delta kept as its
-- own file. Required for any environment using ddl-auto=validate (application-prod);
-- in dev, ddl-auto=update creates the same tables automatically.
--
-- NOTE ON ENUM COLUMNS: status / stage / mode / source columns are plain VARCHAR,
-- not MySQL ENUM, on purpose (the entities force this with @JdbcTypeCode(VARCHAR)).
-- Adding a new status later is then a code-only change - no ALTER TABLE.
-- =============================================================================

USE eems_db;

-- -----------------------------------------------------------------------------
-- TABLE: job_requisition  - a request to hire (manpower request)
-- -----------------------------------------------------------------------------
CREATE TABLE job_requisition (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    requisition_code  VARCHAR(30)   NULL,
    title             VARCHAR(150)  NOT NULL,
    department_id     BIGINT        NOT NULL,
    designation_id    BIGINT        NOT NULL,
    employee_type_id  BIGINT        NOT NULL,
    location_id       BIGINT        NULL,
    openings          INT           NOT NULL,
    filled_count      INT           NOT NULL DEFAULT 0,
    budget_min        DECIMAL(14,2) NULL,
    budget_max        DECIMAL(14,2) NULL,
    min_experience    DECIMAL(4,1)  NULL,
    max_experience    DECIMAL(4,1)  NULL,
    job_description   VARCHAR(4000) NULL,
    justification     VARCHAR(1000) NULL,
    hiring_manager_id BIGINT        NOT NULL,
    recruiter_id      BIGINT        NULL,
    requested_by_id   BIGINT        NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    target_close_date DATE          NULL,
    status_remarks    VARCHAR(500)  NULL,
    version           BIGINT        NULL,
    created_at        DATETIME      NULL,
    updated_at        DATETIME      NULL,
    CONSTRAINT uk_requisition_code UNIQUE (requisition_code),
    CONSTRAINT fk_requisition_department   FOREIGN KEY (department_id)     REFERENCES department(id),
    CONSTRAINT fk_requisition_designation  FOREIGN KEY (designation_id)    REFERENCES designation(id),
    CONSTRAINT fk_requisition_emp_type     FOREIGN KEY (employee_type_id)  REFERENCES employee_type(id),
    CONSTRAINT fk_requisition_location     FOREIGN KEY (location_id)       REFERENCES location(id),
    CONSTRAINT fk_requisition_manager      FOREIGN KEY (hiring_manager_id) REFERENCES employee(id),
    CONSTRAINT fk_requisition_recruiter    FOREIGN KEY (recruiter_id)      REFERENCES employee(id),
    CONSTRAINT fk_requisition_requested_by FOREIGN KEY (requested_by_id)   REFERENCES employee(id),
    INDEX idx_requisition_status (status)
);

-- -----------------------------------------------------------------------------
-- TABLE: requisition_approval - frozen approval stages (same idea as resignation_approval)
-- approver_employee_id is NULL for SKIPPED stages (nobody eligible).
-- -----------------------------------------------------------------------------
CREATE TABLE requisition_approval (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    requisition_id       BIGINT       NOT NULL,
    stage_id             BIGINT       NOT NULL,
    stage_name           VARCHAR(100) NOT NULL,
    sequence_order       INT          NOT NULL,
    approver_employee_id BIGINT       NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    remarks              VARCHAR(500) NULL,
    acted_at             DATETIME     NULL,
    CONSTRAINT fk_reqappr_requisition FOREIGN KEY (requisition_id)       REFERENCES job_requisition(id),
    CONSTRAINT fk_reqappr_stage       FOREIGN KEY (stage_id)             REFERENCES workflow_stage(id),
    CONSTRAINT fk_reqappr_approver    FOREIGN KEY (approver_employee_id) REFERENCES employee(id),
    INDEX idx_reqappr_approver_status (approver_employee_id, status)
);

-- -----------------------------------------------------------------------------
-- TABLE: candidate - a person being considered. NOT an employee.
-- Resume files live in the app.hiring.storage-dir folder, not in the DB and not in /uploads.
-- -----------------------------------------------------------------------------
CREATE TABLE candidate (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name           VARCHAR(50)   NOT NULL,
    last_name            VARCHAR(50)   NULL,
    email                VARCHAR(150)  NOT NULL,
    mobile               VARCHAR(15)   NULL,
    current_company      VARCHAR(150)  NULL,
    current_designation  VARCHAR(100)  NULL,
    current_location     VARCHAR(100)  NULL,
    total_experience     DECIMAL(4,1)  NULL,
    current_ctc          DECIMAL(14,2) NULL,
    expected_ctc         DECIMAL(14,2) NULL,
    notice_period_days   INT           NULL,
    skills               VARCHAR(500)  NULL,
    source               VARCHAR(30)   NOT NULL DEFAULT 'OTHER',
    vendor_name          VARCHAR(150)  NULL,
    referred_by_id       BIGINT        NULL,
    resume_stored_name   VARCHAR(100)  NULL,
    resume_original_name VARCHAR(255)  NULL,
    resume_content_type  VARCHAR(100)  NULL,
    resume_uploaded_at   DATETIME      NULL,
    consent_given_at     DATETIME      NULL,
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    anonymised_at        DATETIME      NULL,
    created_by_id        BIGINT        NULL,
    version              BIGINT        NULL,
    created_at           DATETIME      NULL,
    updated_at           DATETIME      NULL,
    CONSTRAINT uk_candidate_email UNIQUE (email),
    CONSTRAINT fk_candidate_referred_by FOREIGN KEY (referred_by_id) REFERENCES employee(id),
    CONSTRAINT fk_candidate_created_by  FOREIGN KEY (created_by_id)  REFERENCES employee(id),
    INDEX idx_candidate_mobile (mobile),
    INDEX idx_candidate_source (source)
);

-- -----------------------------------------------------------------------------
-- TABLE: job_application - one candidate applying to one requisition (the pipeline row)
-- -----------------------------------------------------------------------------
CREATE TABLE job_application (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id      BIGINT       NOT NULL,
    requisition_id    BIGINT       NOT NULL,
    stage             VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
    rejection_reason  VARCHAR(500) NULL,
    applied_on        DATE         NOT NULL,
    stage_changed_at  DATETIME     NOT NULL,
    hired_employee_id BIGINT       NULL,
    created_by_id     BIGINT       NULL,
    version           BIGINT       NULL,
    created_at        DATETIME     NULL,
    updated_at        DATETIME     NULL,
    CONSTRAINT uk_application_candidate_requisition UNIQUE (candidate_id, requisition_id),
    CONSTRAINT fk_application_candidate   FOREIGN KEY (candidate_id)      REFERENCES candidate(id),
    CONSTRAINT fk_application_requisition FOREIGN KEY (requisition_id)    REFERENCES job_requisition(id),
    CONSTRAINT fk_application_hired_emp   FOREIGN KEY (hired_employee_id) REFERENCES employee(id),
    CONSTRAINT fk_application_created_by  FOREIGN KEY (created_by_id)     REFERENCES employee(id),
    INDEX idx_application_stage (stage)
);

-- -----------------------------------------------------------------------------
-- TABLE: application_stage_history - append-only audit trail of stage changes
-- -----------------------------------------------------------------------------
CREATE TABLE application_stage_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT       NOT NULL,
    from_stage     VARCHAR(20)  NULL,
    to_stage       VARCHAR(20)  NOT NULL,
    changed_by_id  BIGINT       NULL,
    changed_at     DATETIME     NOT NULL,
    remarks        VARCHAR(500) NULL,
    CONSTRAINT fk_history_application FOREIGN KEY (application_id) REFERENCES job_application(id),
    CONSTRAINT fk_history_changed_by  FOREIGN KEY (changed_by_id)  REFERENCES employee(id),
    INDEX idx_history_application (application_id)
);

-- -----------------------------------------------------------------------------
-- TABLE: interview + interview_panelist (panel member AND that member's feedback)
-- -----------------------------------------------------------------------------
CREATE TABLE interview (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id   BIGINT       NOT NULL,
    round_number     INT          NOT NULL,
    round_name       VARCHAR(100) NOT NULL,
    mode             VARCHAR(20)  NOT NULL,
    scheduled_at     DATETIME     NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 60,
    location_or_link VARCHAR(300) NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    cancel_reason    VARCHAR(500) NULL,
    created_by_id    BIGINT       NULL,
    created_at       DATETIME     NULL,
    updated_at       DATETIME     NULL,
    CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES job_application(id),
    CONSTRAINT fk_interview_created_by  FOREIGN KEY (created_by_id)   REFERENCES employee(id),
    INDEX idx_interview_scheduled (status, scheduled_at)
);

CREATE TABLE interview_panelist (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id   BIGINT       NOT NULL,
    interviewer_id BIGINT       NOT NULL,
    rating         INT          NULL,
    recommendation VARCHAR(20)  NULL,
    comments       VARCHAR(2000) NULL,
    submitted_at   DATETIME     NULL,
    CONSTRAINT uk_panelist_interview_interviewer UNIQUE (interview_id, interviewer_id),
    CONSTRAINT fk_panelist_interview   FOREIGN KEY (interview_id)   REFERENCES interview(id),
    CONSTRAINT fk_panelist_interviewer FOREIGN KEY (interviewer_id) REFERENCES employee(id)
);

-- -----------------------------------------------------------------------------
-- TABLE: job_offer + offer_approval
-- -----------------------------------------------------------------------------
CREATE TABLE job_offer (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id          BIGINT        NOT NULL,
    designation_id          BIGINT        NOT NULL,
    annual_ctc              DECIMAL(14,2) NOT NULL,
    joining_date            DATE          NOT NULL,
    expiry_date             DATE          NOT NULL,
    budget_exception_reason VARCHAR(500)  NULL,
    remarks                 VARCHAR(500)  NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    sent_at                 DATETIME      NULL,
    responded_at            DATETIME      NULL,
    closing_reason          VARCHAR(500)  NULL,
    converted_employee_id   BIGINT        NULL,
    created_by_id           BIGINT        NULL,
    version                 BIGINT        NULL,
    created_at              DATETIME      NULL,
    updated_at              DATETIME      NULL,
    CONSTRAINT fk_offer_application FOREIGN KEY (application_id)        REFERENCES job_application(id),
    CONSTRAINT fk_offer_designation FOREIGN KEY (designation_id)        REFERENCES designation(id),
    CONSTRAINT fk_offer_converted   FOREIGN KEY (converted_employee_id) REFERENCES employee(id),
    CONSTRAINT fk_offer_created_by  FOREIGN KEY (created_by_id)         REFERENCES employee(id),
    INDEX idx_offer_status (status, expiry_date)
);

CREATE TABLE offer_approval (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id             BIGINT       NOT NULL,
    stage_id             BIGINT       NOT NULL,
    stage_name           VARCHAR(100) NOT NULL,
    sequence_order       INT          NOT NULL,
    approver_employee_id BIGINT       NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    remarks              VARCHAR(500) NULL,
    acted_at             DATETIME     NULL,
    CONSTRAINT fk_offerappr_offer    FOREIGN KEY (offer_id)             REFERENCES job_offer(id),
    CONSTRAINT fk_offerappr_stage    FOREIGN KEY (stage_id)             REFERENCES workflow_stage(id),
    CONSTRAINT fk_offerappr_approver FOREIGN KEY (approver_employee_id) REFERENCES employee(id),
    INDEX idx_offerappr_approver_status (approver_employee_id, status)
);

-- -----------------------------------------------------------------------------
-- No seed data needed: the REQUISITION and OFFER workflow modules and the
-- "Recruiter" business role are created automatically at startup
-- (PrivilegeModuleInitializer / HiringBootstrap).
-- -----------------------------------------------------------------------------
