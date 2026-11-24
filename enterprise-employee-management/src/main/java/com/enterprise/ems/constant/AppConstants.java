package com.enterprise.ems.constant;

/*
 * PURPOSE: Application-wide constants to avoid magic strings/numbers.
 * Used across security, validation, and business logic layers.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // Role names - must match database roles table
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_USER = "ROLE_USER";

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // File upload
    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final String UPLOAD_DIR = "uploads";

    // Leave status
    public static final String LEAVE_PENDING = "PENDING";
    public static final String LEAVE_APPROVED = "APPROVED";
    public static final String LEAVE_REJECTED = "REJECTED";

    // Attendance status
    public static final String ATTENDANCE_PRESENT = "PRESENT";
    public static final String ATTENDANCE_ABSENT = "ABSENT";
    public static final String ATTENDANCE_HALF_DAY = "HALF_DAY";
    public static final String ATTENDANCE_ON_LEAVE = "ON_LEAVE";

    // Attendance source - who/what recorded the punch
    public static final String ATTENDANCE_SOURCE_SELF = "SELF";
    public static final String ATTENDANCE_SOURCE_ADMIN = "ADMIN";
    public static final String ATTENDANCE_SOURCE_BIOMETRIC = "BIOMETRIC";

    // Biometric device punch types
    public static final String PUNCH_IN = "IN";
    public static final String PUNCH_OUT = "OUT";

    // Default notice period (days) applied when an employee doesn't
    // specify one at submit time. Placeholder until a proper
    // designation/grade-based notice-period policy table exists (flagged
    // as a known v1 gap - see resignation module discussion).
    public static final int DEFAULT_NOTICE_PERIOD_DAYS = 30;

    // Resignation status - see Resignation entity javadoc for the full state
    // machine and the meaning of each transition
    public static final String RESIGNATION_SUBMITTED = "SUBMITTED";
    public static final String RESIGNATION_IN_APPROVAL = "IN_APPROVAL";
    public static final String RESIGNATION_ALL_APPROVALS_DONE = "ALL_APPROVALS_DONE";
    public static final String RESIGNATION_CLEARANCE_PENDING = "CLEARANCE_PENDING";
    public static final String RESIGNATION_CLEARANCE_DONE = "CLEARANCE_DONE";
    public static final String RESIGNATION_FINAL_APPROVED = "FINAL_APPROVED";
    public static final String RESIGNATION_REJECTED = "REJECTED";
    public static final String RESIGNATION_WITHDRAWN = "WITHDRAWN";

    // Resignation approval stage status
    public static final String RESIGNATION_APPROVAL_PENDING = "PENDING";
    public static final String RESIGNATION_APPROVAL_APPROVED = "APPROVED";
    public static final String RESIGNATION_APPROVAL_REJECTED = "REJECTED";
    public static final String RESIGNATION_APPROVAL_SKIPPED = "SKIPPED";

    // Resignation approval stage approverType (WorkflowStage - shared across modules)
    public static final String APPROVER_TYPE_REPORTING_MANAGER = "REPORTING_MANAGER";
    public static final String APPROVER_TYPE_ROLE_IN_DEPARTMENT = "ROLE_IN_DEPARTMENT";
    public static final String APPROVER_TYPE_SPECIFIC_ROLE = "SPECIFIC_ROLE";
    public static final String APPROVER_TYPE_SPECIFIC_USER = "SPECIFIC_USER";

    // PrivilegeModule.code for this module's WorkflowStage chain - see
    // PrivilegeModule / WorkflowStage entity javadocs. ResignationServiceImpl
    // filters WorkflowStageRepository lookups by this constant so it only
    // ever sees stages configured for resignation, even though the table is
    // now shared with other modules.
    public static final String MODULE_CODE_RESIGNATION = "RESIGNATION";
    // Hiring module - approval chains for these two are configured by admin in Workflow Setup.
    public static final String MODULE_CODE_REQUISITION = "REQUISITION";
    public static final String MODULE_CODE_OFFER = "OFFER";

    // Business (workflow) role that marks an employee as a recruiter. Created at startup by
    // HiringBootstrap; admin assigns it to people under Employee Access.
    public static final String WORKFLOW_ROLE_RECRUITER = "Recruiter";

    // Resignation no-dues clearance status
    public static final String CLEARANCE_PENDING = "PENDING";
    public static final String CLEARANCE_CLEARED = "CLEARED";
    public static final String CLEARANCE_BLOCKED = "BLOCKED";
}
