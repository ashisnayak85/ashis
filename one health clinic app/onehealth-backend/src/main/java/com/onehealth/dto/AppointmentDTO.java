package com.onehealth.dto;

import com.onehealth.entity.Appointment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class AppointmentDTO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private Long clinicId;
    private String clinicName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Appointment.AppointmentStatus status;
    private Appointment.BookingSource source;
    private BigDecimal consultationFee;
    private Appointment.PaymentStatus paymentStatus;
}
