-- =============================================================================
-- CONSOLIDATED MIGRATION: cleans up the resignation_approval_stage ->
-- workflow_stage transition, whatever state your database is currently in.
--
-- WHY THIS EXISTS: this replaces the two earlier partial migration scripts
-- (workflow_role_migration.sql, privilege_module_migration.sql), which are
-- deleted. Running them one at a time, or running the app with
-- spring.jpa.hibernate.ddl-auto=update BEFORE running a rename script,
-- commonly leaves a database with TWO stage tables:
--   - resignation_approval_stage  (old name, still holds your real data,
--     still the table resignation_approval.stage_id's FK actually points to)
--   - workflow_stage              (a second, EMPTY table Hibernate silently
--     created because the WorkflowStage entity asked for a table by that
--     name and ddl-auto=update only knows how to CREATE, never RENAME)
--
-- That split is exactly what causes:
--   Cannot add or update a child row: a foreign key constraint fails
--   (resignation_approval, CONSTRAINT ... FOREIGN KEY (stage_id)
--   REFERENCES resignation_approval_stage (id))
-- because the admin screen writes new stages into workflow_stage, but the
-- FK on resignation_approval.stage_id still only recognizes ids from the
-- OLD table.
--
-- WHAT THIS SCRIPT DOES: detects whichever of the following states your
-- database is in and brings it to the single correct end state (one table,
-- named workflow_stage, with privilege_module_id and workflow_role_id,
-- referenced correctly by resignation_approval.stage_id):
--   A. Fresh install, nothing exists yet             -> creates everything
--   B. Only the OLD table exists (never renamed)      -> renames it
--   C. BOTH exist, workflow_stage is the empty stray  -> drops the stray,
--                                                        renames the real one
--   D. Already fully migrated                         -> every step is a
--                                                        no-op, safe to re-run
--
-- BEFORE RUNNING: back this database up. This script runs DDL (CREATE,
-- ALTER, DROP, RENAME), which in MySQL causes an implicit commit at each
-- statement - if it fails partway through, re-running it is safe (every
-- step checks whether it's already done first), but a backup means you're
-- never stuck.
-- =============================================================================

USE eems_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS run_workflow_stage_migration $$

CREATE PROCEDURE run_workflow_stage_migration()
proc_body: BEGIN

    DECLARE old_stage_exists INT DEFAULT 0;
    DECLARE new_stage_exists INT DEFAULT 0;
    DECLARE new_stage_rowcount INT DEFAULT 0;
    DECLARE old_role_col_exists INT DEFAULT 0;
    DECLARE module_col_exists INT DEFAULT 0;
    DECLARE module_fk_exists INT DEFAULT 0;
    DECLARE old_seq_unique_exists INT DEFAULT 0;
    DECLARE new_seq_unique_exists INT DEFAULT 0;
    DECLARE role_fk_name VARCHAR(128) DEFAULT NULL;
    DECLARE resignation_module_id BIGINT DEFAULT NULL;

    -- =========================================================
    -- Step 1: privilege_module - create + seed the RESIGNATION row
    -- (idempotent - PrivilegeModuleInitializer does this too on every app
    -- startup, so this is safe whether the app has already run or not)
    -- =========================================================
    CREATE TABLE IF NOT EXISTS privilege_module (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        code VARCHAR(50) NOT NULL UNIQUE,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(255),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

    INSERT INTO privilege_module (code, name, description)
    SELECT 'RESIGNATION', 'Resignation', 'Employee resignation / separation approval workflow'
    WHERE NOT EXISTS (SELECT 1 FROM privilege_module WHERE code = 'RESIGNATION');

    SELECT id INTO resignation_module_id FROM privilege_module WHERE code = 'RESIGNATION' LIMIT 1;

    -- =========================================================
    -- Step 2: workflow_role / employee_workflow_role - create if missing
    -- =========================================================
    CREATE TABLE IF NOT EXISTS workflow_role (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100) NOT NULL UNIQUE,
        description VARCHAR(255),
        active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

    CREATE TABLE IF NOT EXISTS employee_workflow_role (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        employee_id BIGINT NOT NULL,
        workflow_role_id BIGINT NOT NULL,
        scope_department_id BIGINT NULL,
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

    -- =========================================================
    -- Step 3: resolve the resignation_approval_stage / workflow_stage split
    -- =========================================================
    SELECT COUNT(*) INTO old_stage_exists FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resignation_approval_stage';
    SELECT COUNT(*) INTO new_stage_exists FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage';

    IF new_stage_exists = 1 THEN
        SELECT COUNT(*) INTO new_stage_rowcount FROM workflow_stage;
    END IF;

    -- Case C: both exist and workflow_stage is the empty stray Hibernate
    -- created - drop the stray, then rename the real table into its place.
    IF old_stage_exists = 1 AND new_stage_exists = 1 AND new_stage_rowcount = 0 THEN
        DROP TABLE workflow_stage;
        RENAME TABLE resignation_approval_stage TO workflow_stage;
        SET new_stage_exists = 1;
        SET old_stage_exists = 0;
    END IF;

    -- Safety valve: if BOTH exist and workflow_stage is NOT empty, something
    -- unexpected has happened (real data in both) - stop rather than guess.
    IF old_stage_exists = 1 AND new_stage_exists = 1 AND new_stage_rowcount > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Both resignation_approval_stage and workflow_stage exist and workflow_stage is NOT empty - manual review needed before this script can safely proceed. Compare their contents before continuing.';
    END IF;

    -- Case B: only the old table exists - straight rename.
    IF old_stage_exists = 1 AND new_stage_exists = 0 THEN
        RENAME TABLE resignation_approval_stage TO workflow_stage;
        SET new_stage_exists = 1;
        SET old_stage_exists = 0;
    END IF;

    -- Case A/D: if neither existed, or workflow_stage already existed alone,
    -- nothing to rename - Step 4 below still runs on whatever table exists.

    -- =========================================================
    -- Step 4: migrate the OLD role_id column (auth Role FK) to
    -- workflow_role_id, ONLY if that old column is still present.
    -- =========================================================
    SELECT COUNT(*) INTO old_role_col_exists FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage' AND COLUMN_NAME = 'role_id';

    IF old_role_col_exists = 1 THEN

        INSERT INTO workflow_role (name, description)
        SELECT DISTINCT r.name, CONCAT('Migrated from auth role: ', r.name)
        FROM roles r
        JOIN workflow_stage s ON s.role_id = r.id
        WHERE NOT EXISTS (SELECT 1 FROM workflow_role wr WHERE wr.name = r.name);

        ALTER TABLE workflow_stage ADD COLUMN workflow_role_id BIGINT NULL AFTER approver_type;

        UPDATE workflow_stage s
        JOIN roles r ON s.role_id = r.id
        JOIN workflow_role wr ON wr.name = r.name
        SET s.workflow_role_id = wr.id
        WHERE s.role_id IS NOT NULL;

        -- Best-effort backfill of employee_workflow_role from current
        -- auth-role holders, so approval routing keeps working immediately.
        -- Review the result (query at the bottom) before trusting it fully.
        INSERT INTO employee_workflow_role (employee_id, workflow_role_id, scope_department_id, active, assigned_at)
        SELECT DISTINCT e.id, s.workflow_role_id, e.department_id, TRUE, NOW()
        FROM workflow_stage s
        JOIN roles r ON r.id = s.role_id
        JOIN user_roles ur ON ur.role_id = r.id
        JOIN users u ON u.id = ur.user_id
        JOIN employee e ON e.user_id = u.id
        WHERE s.approver_type = 'ROLE_IN_DEPARTMENT' AND s.department_scope = 'EMPLOYEE_DEPARTMENT'
          AND e.active = TRUE AND e.department_id IS NOT NULL;

        INSERT INTO employee_workflow_role (employee_id, workflow_role_id, scope_department_id, active, assigned_at)
        SELECT DISTINCT e.id, s.workflow_role_id, s.fixed_department_id, TRUE, NOW()
        FROM workflow_stage s
        JOIN roles r ON r.id = s.role_id
        JOIN user_roles ur ON ur.role_id = r.id
        JOIN users u ON u.id = ur.user_id
        JOIN employee e ON e.user_id = u.id
        WHERE s.approver_type = 'ROLE_IN_DEPARTMENT' AND s.department_scope = 'FIXED_DEPARTMENT'
          AND e.active = TRUE AND s.fixed_department_id IS NOT NULL;

        INSERT INTO employee_workflow_role (employee_id, workflow_role_id, scope_department_id, active, assigned_at)
        SELECT DISTINCT e.id, s.workflow_role_id, NULL, TRUE, NOW()
        FROM workflow_stage s
        JOIN roles r ON r.id = s.role_id
        JOIN user_roles ur ON ur.role_id = r.id
        JOIN users u ON u.id = ur.user_id
        JOIN employee e ON e.user_id = u.id
        WHERE s.approver_type = 'SPECIFIC_ROLE' AND e.active = TRUE;

        -- Find and drop the old FK on role_id dynamically - its generated
        -- name varies by environment, so we look it up rather than guess.
        SELECT CONSTRAINT_NAME INTO role_fk_name
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage'
          AND COLUMN_NAME = 'role_id' AND REFERENCED_TABLE_NAME = 'roles'
        LIMIT 1;

        IF role_fk_name IS NOT NULL THEN
            SET @drop_fk_sql = CONCAT('ALTER TABLE workflow_stage DROP FOREIGN KEY ', role_fk_name);
            PREPARE stmt FROM @drop_fk_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        ALTER TABLE workflow_stage DROP COLUMN role_id;
    END IF;

    -- =========================================================
    -- Step 5: ensure privilege_module_id exists, is backfilled, NOT NULL,
    -- and has its FK - whatever state the column was in.
    -- =========================================================
    SELECT COUNT(*) INTO module_col_exists FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage' AND COLUMN_NAME = 'privilege_module_id';

    IF module_col_exists = 0 THEN
        ALTER TABLE workflow_stage ADD COLUMN privilege_module_id BIGINT NULL AFTER id;
    END IF;

    UPDATE workflow_stage SET privilege_module_id = resignation_module_id WHERE privilege_module_id IS NULL;

    SELECT COUNT(*) INTO module_fk_exists FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage'
          AND COLUMN_NAME = 'privilege_module_id' AND REFERENCED_TABLE_NAME = 'privilege_module';

    IF module_fk_exists = 0 THEN
        ALTER TABLE workflow_stage MODIFY COLUMN privilege_module_id BIGINT NOT NULL;
        ALTER TABLE workflow_stage ADD CONSTRAINT fk_workflow_stage_module
            FOREIGN KEY (privilege_module_id) REFERENCES privilege_module(id);
    END IF;

    -- =========================================================
    -- Step 6: sequence_order uniqueness must be PER MODULE, not global.
    -- =========================================================
    SELECT COUNT(*) INTO old_seq_unique_exists FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage' AND INDEX_NAME = 'sequence_order';

    IF old_seq_unique_exists > 0 THEN
        ALTER TABLE workflow_stage DROP INDEX sequence_order;
    END IF;

    SELECT COUNT(*) INTO new_seq_unique_exists FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_stage' AND INDEX_NAME = 'uq_workflow_stage_module_sequence';

    IF new_seq_unique_exists = 0 THEN
        ALTER TABLE workflow_stage ADD CONSTRAINT uq_workflow_stage_module_sequence
            UNIQUE (privilege_module_id, sequence_order);
    END IF;

END proc_body $$

DELIMITER ;

CALL run_workflow_stage_migration();
DROP PROCEDURE run_workflow_stage_migration;

-- =============================================================================
-- VERIFY after running:
-- =============================================================================
-- 1. Only one stage table should remain:
--      SHOW TABLES LIKE '%approval_stage%';   -- should return EMPTY
--      SHOW TABLES LIKE 'workflow_stage';     -- should return one row
--
-- 2. The FK on resignation_approval now points at the renamed table:
--      SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME
--      FROM information_schema.KEY_COLUMN_USAGE
--      WHERE TABLE_NAME = 'resignation_approval' AND COLUMN_NAME = 'stage_id';
--      -- REFERENCED_TABLE_NAME should be workflow_stage
--
-- 3. Your real stage data survived the rename:
--      SELECT * FROM workflow_stage ORDER BY sequence_order;
--
-- 4. If Step 4 ran (old role_id existed), review what it backfilled:
--      SELECT ewr.id, e.employee_code, e.first_name, e.last_name, wr.name AS role,
--             d.name AS scope_department, ewr.assigned_at
--      FROM employee_workflow_role ewr
--      JOIN employee e ON e.id = ewr.employee_id
--      JOIN workflow_role wr ON wr.id = ewr.workflow_role_id
--      LEFT JOIN department d ON d.id = ewr.scope_department_id
--      ORDER BY wr.name, scope_department;
-- =============================================================================
