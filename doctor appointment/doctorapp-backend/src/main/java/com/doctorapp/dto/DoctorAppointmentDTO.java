package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Doctor-facing view of an appointment: shows the patient's details instead of the doctor's. */
@Getter
@Builder
@AllArgsConstructor
public class DoctorAppointmentDTO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String clinicName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String paymentStatus;
    private BigDecimal consultationFee;
}
