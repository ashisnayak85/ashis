package com.enterprise.ems.dto;

import lombok.*;

/*
 * PURPOSE: Personal dashboard for a plain ROLE_USER employee - scoped entirely
 * to their own attendance/leave history for the current month. Deliberately
 * carries none of the org-wide numbers in DashboardStats (headcount, total
 * departments, etc.) - an employee has no business seeing those.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyDashboardStats {

    private String employeeName;

    private long presentDaysThisMonth;
    private long absentDaysThisMonth;
    private long halfDaysThisMonth;
    private long onLeaveDaysThisMonth;

    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;
}
