package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Builder
public class AvailabilityResponse {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long clinicId;
    private String clinicName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private boolean active;

    // Only populated when this response is returned from an operation that
    // changed the schedule (add / activate / deactivate) - null on plain reads
    // (getMyAvailability / getClinicAvailability).
    private SlotResyncResultDTO resync;
}
