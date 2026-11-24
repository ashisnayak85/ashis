package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** One point on the "appointments over time" line chart. */
@Getter
@Builder
public class TrendPointDTO {
    private LocalDate date;
    private long total;
    private long completed;
    private long noShow;
    private long walkIns;
    private long onlineBookings;
}
