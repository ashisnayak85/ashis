package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AvailabilityAuditLogDTO {
    private Long id;
    private String doctorName;
    private String clinicName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String action;
    private String performedByName;
    private String performedByRole;
    private String note;
    private LocalDateTime performedAt;
}
