package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The clinic admin's receipt after billing a prescription. Deliberately
 * carries ONLY selling-price data (what the patient is charged) - never cost
 * price or profit, which are owner-only (see MedicineService.billPrescription
 * and PrescriptionItem javadoc).
 */
@Getter
@Builder
public class BillReceiptDTO {
    private Long prescriptionId;
    private Long appointmentId;
    private LocalDateTime billedAt;
    private List<BillReceiptItemDTO> items;
    private BigDecimal totalAmount;
}
