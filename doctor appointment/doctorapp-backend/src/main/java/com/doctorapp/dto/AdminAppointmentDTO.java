package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Admin-facing view of an appointment, showing both patient and doctor names. */
@Getter
@Builder
@AllArgsConstructor
public class AdminAppointmentDTO {
    private Long id;
    private String patientName;
    private String doctorName;
    private String clinicName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String paymentStatus;
    private BigDecimal consultationFee;
}
