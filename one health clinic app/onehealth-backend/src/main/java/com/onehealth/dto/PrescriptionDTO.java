package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OWNER-ONLY full view of a prescription, including cost/profit. Built only
 * from BILLED items (see PrescriptionItem) - a prescription that's still
 * PRESCRIBED (not yet billed) has no priced items yet and totals of zero.
 * Doctor and clinic-admin facing code should use PrescriptionSummaryDTO
 * (unbilled, no price) or BillReceiptDTO (billed, selling price only)
 * instead - never this one.
 */
@Getter
@Builder
public class PrescriptionDTO {
    private Long id;
    private Long appointmentId;
    private String status;
    private LocalDateTime createdAt;
    private List<PrescriptionItemDTO> items;
    private BigDecimal totalAmount;
    private BigDecimal totalProfit;
}
