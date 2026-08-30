package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class SlotDTO {
    private Long slotId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
}
