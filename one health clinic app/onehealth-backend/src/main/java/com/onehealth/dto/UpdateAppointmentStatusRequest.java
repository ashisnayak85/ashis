package com.onehealth.dto;

import com.onehealth.entity.Appointment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {
    @NotNull
    private Appointment.AppointmentStatus status;
}
