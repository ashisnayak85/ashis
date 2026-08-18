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
}
