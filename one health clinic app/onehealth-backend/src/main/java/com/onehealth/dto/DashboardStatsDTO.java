package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Top-level payload for the owner (or clinic-admin) dashboard screen. */
@Getter
@Builder
public class DashboardStatsDTO {
    private LocalDate from;
    private LocalDate to;

    private long totalAppointments;
    private long totalOnlineBookings;
    private long totalWalkIns;
    private long totalCompleted;
    private long totalNoShow;
    private long totalCancelled;
    private double overallCompletionRatePercent;
    private BigDecimal totalRevenue;
    private long totalUniquePatients;
    private int activeClinicCount;
    private int activeDoctorCount;

    // Location-wise breakdown - this drives the "% patients came to location X
    // and got service" chart, per branch, for the selected range.
    private List<ClinicStatsDTO> byClinic;

    private List<DoctorUtilizationDTO> doctorUtilization;

    // Daily trend within the range, for a line/bar chart of volume over time.
    private List<TrendPointDTO> trend;

    // If any individual section below failed to compute (e.g. bad/inconsistent
    // data for one clinic), it's reported here as a short message and that
    // section is returned empty/zeroed - rather than the whole dashboard
    // failing to load. The frontend should surface these as small "couldn't
    // load this section" notices, not block rendering the rest of the page.
    @Builder.Default
    private List<String> sectionWarnings = List.of();
}
