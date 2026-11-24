package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * One (clinic, medicine) row on the owner's reconciliation report: how much
 * HQ dispatched there in total, how much the system has recorded as sold,
 * and what the system currently believes is left on the shelf.
 *
 * `systemRemaining` should always equal dispatched-minus-sold if every
 * dispensing event went through the app - it's derived the same way
 * internally. What this report is really for is comparing it against a
 * clinic's PHYSICAL count: if the shelf has less than systemRemaining says
 * it should, that gap is shrinkage - miscounting, spoilage, or theft - and is
 * exactly what a self-reported "clinic buys its own stock" model can't
 * surface, since there's nothing independent to compare against.
 */
@Getter
@Builder
public class StockReconciliationDTO {
    private Long clinicId;
    private String clinicName;
    private Long medicineId;
    private String medicineName;
    private String unit;

    private long totalDispatched;
    private long totalSold;
    private long systemRemaining; // totalDispatched - totalSold, should match ClinicMedicineStock.quantityOnHand
}
