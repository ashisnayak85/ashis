package com.onehealth.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Query params for owner/clinic-admin dashboard endpoints. `from`/`to` are
 * optional - if either is missing, the service defaults BOTH to "today", per
 * the requirement that the dashboard loads current-day data by default.
 * `clinicId` optional - null means "all branches" (owner view); a clinic admin
 * is always forced to their own clinicId server-side regardless of what's passed.
 */
@Data
public class DashboardFilter {
    private LocalDate from;
    private LocalDate to;
    private Long clinicId;
}
