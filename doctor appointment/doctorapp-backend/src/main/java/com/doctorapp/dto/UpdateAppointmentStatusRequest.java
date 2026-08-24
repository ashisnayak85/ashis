package com.doctorapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAppointmentStatusRequest {
    /** One of: COMPLETED, CANCELLED, NO_SHOW (BOOKED is not allowed as a target status). */
    @NotBlank
    private String status;
}
