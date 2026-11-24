-- =============================================================================
-- EEMS Database Schema - Resignation / Separation module
-- Run this AFTER schema.sql (adds to the existing eems_db). Kept as a
-- separate file rather than merged into schema.sql so it's a clean,
-- reviewable delta - fold it into schema.sql once applied, or wire it
-- through Flyway/Liquibase if/when this project adopts one (see the
-- application-prod.properties comment on schema management).
-- =============================================================================

USE eems_db;

-- -----------------------------------------------------------------------------
-- Extend employee with a self-referencing manager link.
-- WHY: needed for the REPORTING_MANAGER approver type below. Nullable so
-- existing rows and top-of-org employees (e.g. the CEO) don't break.
-- -----------------------------------------------------------------------------
ALTER TABLE employee
    ADD COLUMN reporting_manager_id BIGINT NULL,
    ADD CONSTRAINT fk_employee_reporting_manager
        FOREIGN KEY (reporting_manager_id) REFERENCES employee(id);

-- -----------------------------------------------------------------------------
-- TABLE: workflow_role
-- WHY: Admin-managed BUSINESS role master (HOD, HR Head, Project Head, ...)
-- used for approval routing - deliberately SEPARATE from `roles` (which
-- drives Spring Security login permissions). Admin adds/edits/retires rows
-- here via the API (AdminWorkflowRoleController) - no code change needed to
-- introduce a new business role. See WorkflowRole.java entity javadoc.
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: employee_workflow_role
-- WHY: "Employee Access" - admin-managed assignment of a workflow_role to an
-- employee, optionally scoped to one department (NULL = company-wide).
-- ResignationServiceImpl.resolveApprover() resolves ROLE_IN_DEPARTMENT /
-- SPECIFIC_ROLE stages against THIS table, not against Spring Security
-- roles. Revoked by setting active=FALSE, never deleted - keeps history.
-- See EmployeeWorkflowRole.java entity javadoc.
-- -----------------------------------------------------------------------------
CREATE TABLE employee_workflow_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    workflow_role_id BIGINT NOT NULL,
    scope_department_id BIGINT NULL,               -- NULL = global/company-wide
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at DATETIME NOT NULL,
    assigned_by_employee_id BIGINT NULL,
    removed_at DATETIME NULL,
    removed_by_employee_id BIGINT NULL,
    FOREIGN KEY (employee_id) REFERENCES employee(id),
    FOREIGN KEY (workflow_role_id) REFERENCES workflow_role(id),
    FOREIGN KEY (scope_department_id) REFERENCES department(id),
    FOREIGN KEY (assigned_by_employee_id) REFERENCES employee(id),
    FOREIGN KEY (removed_by_employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

CREATE INDEX idx_employee_workflow_role_employee ON employee_workflow_role(employee_id, active);
CREATE INDEX idx_employee_workflow_role_lookup ON employee_workflow_role(workflow_role_id, scope_department_id, active);

-- -----------------------------------------------------------------------------
-- TABLE: privilege_module
-- WHY: Master list of modules that can have their own admin-configured
-- approval workflow (RESIGNATION today; LEAVE/EXPENSE/... in future). Lets
-- workflow_stage below be SHARED across every such module instead of each
-- one getting its own stage table + admin screen + sidebar entry. See
-- PrivilegeModule.java entity javadoc.
-- -----------------------------------------------------------------------------
CREATE TABLE privilege_module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,        -- 'RESIGNATION', 'LEAVE', 'EXPENSE', ...
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: workflow_stage
-- WHY: Admin-configurable approval-chain POLICY, scoped to a privilege_module
-- and reused by every workflow instance of that module while active -
-- decouples "what stages exist / who resolves each one, for which module"
-- from code. Fully CRUD-able via AdminWorkflowStageController - no manual
-- SQL required. Formerly "resignation_approval_stage" (resignation-only,
-- no module concept) - see WorkflowStage.java entity javadoc.
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_stage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    privilege_module_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sequence_order INT NOT NULL,
    approver_type VARCHAR(30) NOT NULL,          -- REPORTING_MANAGER / ROLE_IN_DEPARTMENT / SPECIFIC_ROLE / SPECIFIC_USER
    workflow_role_id BIGINT NULL,                  -- used by ROLE_IN_DEPARTMENT / SPECIFIC_ROLE - references workflow_role, NOT roles
    department_scope VARCHAR(30) NULL,             -- EMPLOYEE_DEPARTMENT / FIXED_DEPARTMENT - used by ROLE_IN_DEPARTMENT
    fixed_department_id BIGINT NULL,               -- used when department_scope = FIXED_DEPARTMENT
    specific_employee_id BIGINT NULL,              -- used by SPECIFIC_USER
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- sequence_order is unique PER MODULE, not globally - two different
    -- modules can both have a "stage 1".
    UNIQUE KEY uq_workflow_stage_module_sequence (privilege_module_id, sequence_order),
    FOREIGN KEY (privilege_module_id) REFERENCES privilege_module(id),
    FOREIGN KEY (workflow_role_id) REFERENCES workflow_role(id),
    FOREIGN KEY (fixed_department_id) REFERENCES department(id),
    FOREIGN KEY (specific_employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: resignation
-- WHY: Header record for a resignation request. See Resignation.java javadoc
-- for the full status state machine.
-- -----------------------------------------------------------------------------
CREATE TABLE resignation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    reason VARCHAR(1000),
    requested_on DATE NOT NULL,
    notice_period_days INT NOT NULL,
    expected_last_working_day DATE NOT NULL,
    actual_last_working_day DATE NULL,
    override_reason VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

-- Enforce "one active resignation per employee at a time" at the app layer
-- (ResignationRepository.findByEmployeeIdAndStatusNotIn) - not a DB
-- constraint, since MySQL can't easily express "unique while status NOT IN (...)".
CREATE INDEX idx_resignation_employee ON resignation(employee_id);
CREATE INDEX idx_resignation_status ON resignation(status);

-- -----------------------------------------------------------------------------
-- TABLE: resignation_approval
-- WHY: Per-stage approval instances, resolved from workflow_stage (filtered
-- to the RESIGNATION module) at submit time and frozen - the audit trail of
-- who was asked and what they decided. Mirrors ticket_conversation's role
-- for ticket_master.
-- -----------------------------------------------------------------------------
CREATE TABLE resignation_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resignation_id BIGINT NOT NULL,
    stage_id BIGINT NOT NULL,
    stage_name VARCHAR(100) NOT NULL,
    sequence_order INT NOT NULL,
    approver_employee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED / SKIPPED
    remarks VARCHAR(500),
    acted_at DATETIME NULL,
    FOREIGN KEY (resignation_id) REFERENCES resignation(id) ON DELETE CASCADE,
    FOREIGN KEY (stage_id) REFERENCES workflow_stage(id),
    FOREIGN KEY (approver_employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

CREATE INDEX idx_resignation_approval_resignation ON resignation_approval(resignation_id);
CREATE INDEX idx_resignation_approval_approver ON resignation_approval(approver_employee_id, status);

-- -----------------------------------------------------------------------------
-- TABLE: resignation_clearance
-- WHY: No-dues clearance, one row per clearing department (IT, Finance,
-- Admin, HR...) - each clears independently rather than one flat boolean.
-- -----------------------------------------------------------------------------
CREATE TABLE resignation_clearance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resignation_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / CLEARED / BLOCKED
    cleared_by_employee_id BIGINT NULL,
    remarks VARCHAR(500),
    cleared_at DATETIME NULL,
    FOREIGN KEY (resignation_id) REFERENCES resignation(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id),
    FOREIGN KEY (cleared_by_employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

CREATE INDEX idx_resignation_clearance_resignation ON resignation_clearance(resignation_id);
CREATE INDEX idx_resignation_clearance_department ON resignation_clearance(department_id, status);

-- -----------------------------------------------------------------------------
-- TABLE: resignation_asset
-- WHY: Lightweight asset-return line items surfaced during no-dues. Not a
-- full asset-tracking module - see ResignationAsset.java javadoc.
-- -----------------------------------------------------------------------------
CREATE TABLE resignation_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resignation_id BIGINT NOT NULL,
    asset_description VARCHAR(255) NOT NULL,
    returned BOOLEAN NOT NULL DEFAULT FALSE,
    remarks VARCHAR(500),
    returned_at DATETIME NULL,
    FOREIGN KEY (resignation_id) REFERENCES resignation(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_resignation_asset_resignation ON resignation_asset(resignation_id);

-- =============================================================================
-- SAMPLE DATA - this is only meant to make the feature testable out of the
-- box on a brand new install. In real use, admin manages workflow_role,
-- workflow_stage, and employee_workflow_role through the API - see
-- AdminWorkflowRoleController, AdminWorkflowStageController,
-- AdminEmployeeWorkflowRoleController.
--
-- privilege_module is NOT admin-manageable and NOT seeded here by hand -
-- PrivilegeModuleInitializer registers the RESIGNATION row (and any future
-- module in the SystemModule enum) automatically on application startup.
-- Run this schema BEFORE first starting the app, or run the app once
-- first and skip straight to the workflow_role / workflow_stage inserts
-- below - either order works since this INSERT is idempotent.
--
-- A 3-stage chain for the RESIGNATION module: Reporting Manager -> HOD (in
-- the employee's own department) -> HR Head (fixed to the HR department
-- regardless of who's resigning). Adjust to your real org chart.
-- =============================================================================

INSERT INTO privilege_module (code, name, description)
SELECT 'RESIGNATION', 'Resignation', 'Employee resignation / separation approval workflow'
WHERE NOT EXISTS (SELECT 1 FROM privilege_module WHERE code = 'RESIGNATION');

INSERT INTO workflow_role (name, description) VALUES
('HOD', 'Head of Department - approves resignations for their own department'),
('HR Head', 'Final HR approval on resignations, routed to the HR department regardless of the resigning employee''s own department');

INSERT INTO workflow_stage (privilege_module_id, name, sequence_order, approver_type, workflow_role_id, department_scope, fixed_department_id)
VALUES
((SELECT id FROM privilege_module WHERE code = 'RESIGNATION'), 'Reporting Manager', 1, 'REPORTING_MANAGER', NULL, NULL, NULL),
((SELECT id FROM privilege_module WHERE code = 'RESIGNATION'), 'HOD Approval', 2, 'ROLE_IN_DEPARTMENT',
    (SELECT id FROM workflow_role WHERE name = 'HOD'), 'EMPLOYEE_DEPARTMENT', NULL),
((SELECT id FROM privilege_module WHERE code = 'RESIGNATION'), 'HR Final Approval', 3, 'ROLE_IN_DEPARTMENT',
    (SELECT id FROM workflow_role WHERE name = 'HR Head'), 'FIXED_DEPARTMENT',
    (SELECT id FROM department WHERE code = 'HR'));

-- NOTE: workflow_role rows above have no employee_workflow_role assignments
-- yet - admin must assign "HOD" and "HR Head" to the relevant employees via
-- POST /api/admin/employee-roles before submitting a resignation, or those
-- stages will resolve to SKIPPED (see ResignationServiceImpl.resolveApprover).
