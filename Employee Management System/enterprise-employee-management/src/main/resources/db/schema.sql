-- =============================================================================
-- EEMS Database Schema
-- Enterprise Employee Management System
-- =============================================================================

CREATE DATABASE IF NOT EXISTS eems_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eems_db;

-- -----------------------------------------------------------------------------
-- TABLE: roles
-- WHY: Defines permission levels for Role-Based Access Control (RBAC)
-- Used by Spring Security to authorize API/page access
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: users
-- WHY: Authentication credentials separate from employee HR data
-- A user may or may not be linked to an employee record
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: user_roles (Many-to-Many join table)
-- WHY: A user can have multiple roles; a role can belong to multiple users
-- -----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: department
-- WHY: Organizational structure; employees belong to departments
-- -----------------------------------------------------------------------------
CREATE TABLE department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: designation
-- WHY: Job titles (e.g. Software Engineer, HR Manager) - previously a free-text
--      field on employee, moved to its own managed table (same pattern as
--      department) so titles stay consistent and can be added/retired centrally.
-- -----------------------------------------------------------------------------
CREATE TABLE designation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: employee
-- WHY: Core HR entity - personal and employment information
-- -----------------------------------------------------------------------------
CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    mobile VARCHAR(15),
    date_of_birth DATE,
    date_of_joining DATE,
    salary DECIMAL(12,2),
    gender VARCHAR(10),
    -- Qualification & experience
    qualification VARCHAR(100),
    year_of_passing INT,
    total_experience_years DECIMAL(4,1),
    marital_status VARCHAR(20),
    aadhar_number VARCHAR(12) UNIQUE,
    salary_calculated_from DATE,
    -- Present address
    present_address_line VARCHAR(255),
    present_city_district VARCHAR(100),
    present_state VARCHAR(100),
    present_pincode VARCHAR(10),
    -- Permanent address
    permanent_address_line VARCHAR(255),
    permanent_city_district VARCHAR(100),
    permanent_state VARCHAR(100),
    permanent_pincode VARCHAR(10),
    -- Bank details
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(30),
    bank_ifsc_code VARCHAR(11),
    -- Statutory information (PF / ESI)
    pf_applicable BOOLEAN,
    pf_number VARCHAR(30),
    uan_number VARCHAR(20),
    restrict_pf BOOLEAN,
    zero_pension BOOLEAN,
    zero_pt BOOLEAN,
    esi_applicable BOOLEAN,
    esi_number VARCHAR(30),
    esi_dispensation BOOLEAN,
    qualification_certificate_file_id BIGINT,
    profile_photo VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    department_id BIGINT,
    designation_id BIGINT,
    user_id BIGINT UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(id),
    FOREIGN KEY (designation_id) REFERENCES designation(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: attendance
-- WHY: Daily attendance tracking for payroll and compliance
-- UNIQUE constraint prevents duplicate entries per employee per day
-- -----------------------------------------------------------------------------
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employee(id),
    UNIQUE KEY uk_employee_date (employee_id, attendance_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: leave_master
-- WHY: Leave request workflow with approval status
-- -----------------------------------------------------------------------------
CREATE TABLE leave_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: file_upload
-- WHY: Metadata for uploaded files (actual files stored on disk)
-- -----------------------------------------------------------------------------
CREATE TABLE file_upload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploaded_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- TABLE: audit_log
-- WHY: Compliance and security audit trail
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    performed_by VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =============================================================================
-- SAMPLE DATA
-- Passwords are BCrypt encoded: admin123, manager123
-- =============================================================================

INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'System Administrator'),
('ROLE_MANAGER', 'Department Manager'),
('ROLE_USER', 'Regular Employee');

INSERT INTO users (username, password, email, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@eems.com', TRUE),
('manager', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQW5a4qRzqJ8qJ8qJ8qJ8qJ8qJ8qJ8', 'manager@eems.com', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1), (2, 2);

INSERT INTO department (name, code, description) VALUES
('Information Technology', 'IT', 'Software development and infrastructure'),
('Human Resources', 'HR', 'Recruitment and employee welfare'),
('Finance', 'FIN', 'Accounting and financial planning');

-- NOTE: designation_id references the designation table, which must be
-- populated first (e.g. via the app's Designation screen or its own INSERT)
-- before employee rows can point at real designation ids.
INSERT INTO employee (employee_code, first_name, last_name, email, mobile, date_of_joining, salary, department_id) VALUES
('EMP001', 'John', 'Doe', 'john.doe@eems.com', '9876543210', '2023-01-15', 75000.00, 1),
('EMP002', 'Jane', 'Smith', 'jane.smith@eems.com', '9876543211', '2022-06-01', 85000.00, 2),
('EMP003', 'Robert', 'Johnson', 'robert.j@eems.com', '9876543212', '2021-03-10', 95000.00, 3);

INSERT INTO attendance (employee_id, attendance_date, check_in_time, check_out_time, status) VALUES
(1, CURDATE(), '09:00:00', '18:00:00', 'PRESENT'),
(2, CURDATE(), '09:15:00', '18:30:00', 'PRESENT');

INSERT INTO leave_master (employee_id, leave_type, start_date, end_date, reason, status) VALUES
(1, 'CASUAL', DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY), 'Personal work', 'PENDING');
