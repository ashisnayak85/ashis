package com.onehealth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class AvailabilityRequest {
    @NotNull
    private Long clinicId;

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    // Also enforced (with a clearer, context-aware message) in SlotService -
    // this annotation is the first line of defense so a bad value gets
    // rejected with a clean 400 before it reaches any business logic at all.
    @Min(value = 1, message = "Slot length must be at least 1 minute.")
    private Integer slotDurationMinutes;
}
