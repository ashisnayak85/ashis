package com.enterprise.ems.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    private long totalEmployees;
    private long totalDepartments;
    private long presentToday;
    private long pendingLeaves;
}
