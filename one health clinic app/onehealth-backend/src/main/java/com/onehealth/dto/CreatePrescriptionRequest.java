package com.onehealth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Doctor-facing: write a prescription for an appointment. This ONLY records
 * what's clinically recommended - no stock is touched and no price is
 * computed here. The clinic admin bills it separately (see
 * BillPrescriptionRequest), possibly for less than prescribed.
 */
@Data
public class CreatePrescriptionRequest {
    @NotEmpty(message = "Add at least one medicine to the prescription")
    @Valid
    private List<PrescriptionItemRequest> items;
}
