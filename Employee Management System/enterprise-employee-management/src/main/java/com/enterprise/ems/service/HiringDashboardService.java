package com.enterprise.ems.service;

import com.enterprise.ems.dto.HiringViews;
import com.enterprise.ems.service.hiring.HiringActor;

public interface HiringDashboardService {

    // Hiring staff only.
    HiringViews.Dashboard getDashboard(HiringActor actor);

    // What the signed-in person can do + their inbox counts (drives the sidebar).
    HiringViews.Me me(HiringActor actor);

    // Everything the hiring forms need in one call.
    HiringViews.Lookups lookups(HiringActor actor);

    // Employee picker for hiring manager / interviewers.
    java.util.List<HiringViews.EmployeeLookup> searchEmployees(String keyword, HiringActor actor);
}
