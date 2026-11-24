package com.onehealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    // Only validated when present (a brand-new walk-in) - an existing
    // patientId doesn't need this field re-supplied.
    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String patientPhone;

    // Added for location-based business analytics (see Patient entity javadoc).
    // Optional at walk-in time - front desk can leave it blank under time
    // pressure, but the field is here so it CAN be captured at intake.
    private String patientCity;
    private String patientPincode;
}
