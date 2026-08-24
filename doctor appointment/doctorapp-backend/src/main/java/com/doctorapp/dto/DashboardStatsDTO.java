package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalDoctors;
    private long verifiedDoctors;
    private long pendingDoctors;
    private long totalPatients;
    private long totalAppointments;
    private long todayAppointments;
    private long totalClinics;
    private long verifiedClinics;
    private long pendingClinics;
}
