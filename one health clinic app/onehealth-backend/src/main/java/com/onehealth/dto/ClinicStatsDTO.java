package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** One branch's numbers for the selected date range - the row behind the location-wise % chart. */
@Getter
@Builder
public class ClinicStatsDTO {
    private Long clinicId;
    private String clinicName;
    private String city;

    private long totalAppointments;
    private long onlineBookings;
    private long walkIns;
    private long completed;
    private long noShow;
    private long cancelled;
    private long stillBooked; // booked, appointment date hasn't happened / not marked yet

    // completed / (total - cancelled), i.e. of everyone who actually had a
    // standing appointment, what % came in and got served. Cancelled bookings
    // are excluded from the denominator since the patient proactively opted out
    // before ever being expected - counting them as "missed service" would
    // conflate cancellation behavior with no-show/service-delivery behavior.
    private double completionRatePercent;

    private BigDecimal revenue;
    private long uniquePatients;
}
