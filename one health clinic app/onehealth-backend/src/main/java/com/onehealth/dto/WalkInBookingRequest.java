package com.onehealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Used by a clinic admin to book a patient who walked in without a prior
 * booking. If `patientId` is given, we book against that existing record;
 * otherwise we create a new Patient from name/phone (no login required).
 */
@Data
public class WalkInBookingRequest {
    @NotNull
    private Long slotId;

    private Long patientId;

    private String patientName;
    private String patientPhone;
}
