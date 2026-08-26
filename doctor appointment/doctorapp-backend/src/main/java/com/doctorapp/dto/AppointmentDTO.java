package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class AppointmentDTO {
    private Long id;
    private String doctorName;
    private String clinicName;
    private String clinicAddress;
    private Double clinicLatitude;
    private Double clinicLongitude;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private BigDecimal consultationFee;
    private String paymentStatus;
    private Long doctorId;
    /** True once the patient has submitted a rating for this appointment. Drives the "Rate your visit" prompt. */
    private boolean rated;
}
