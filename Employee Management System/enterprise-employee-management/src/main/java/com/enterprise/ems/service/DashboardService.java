package com.enterprise.ems.service;

import com.enterprise.ems.dto.DashboardStats;
import com.enterprise.ems.dto.MyDashboardStats;

public interface DashboardService {

    DashboardStats getStats();

    // Employee-scoped equivalent for a plain ROLE_USER login. employeeName is
    // passed in rather than looked up again since the caller (controller) already
    // resolved the Employee entity via CurrentEmployeeResolver.
    MyDashboardStats getMyStats(Long employeeId, String employeeName);
}
