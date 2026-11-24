package com.onehealth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * One line the clinic admin is choosing to actually bill/dispense. `quantity`
 * may be LESS than what the doctor prescribed for this medicine (e.g. a
 * patient can only afford 6 of the 10 prescribed tablets) - it can never be
 * more, enforced server-side in MedicineService.billPrescription.
 */
@Data
public class BillLineRequest {
    @NotNull
    private Long medicineId;

    @NotNull
    @Min(value = 1, message = "Billed quantity must be at least 1 - drop the line instead of billing zero")
    private Integer quantity;
}
