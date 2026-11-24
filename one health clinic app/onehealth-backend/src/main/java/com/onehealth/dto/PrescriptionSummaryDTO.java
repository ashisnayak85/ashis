package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A prescription with NO pricing - what the doctor wrote, and what a clinic
 * admin sees before billing it. Used by:
 *  - the doctor, to view their own prescription for an appointment
 *  - the clinic admin, for the pending-bills queue and to review one
 *    prescription before deciding what to actually charge/dispense
 *
 * Once billed, the priced record lives separately (see BillReceiptDTO for
 * the admin's receipt, or PrescriptionDTO for the owner's full cost/profit view).
 */
@Getter
@Builder
public class PrescriptionSummaryDTO {
    private Long id;
    private Long appointmentId;
    private Long clinicId;
    private String patientName;
    private String doctorName;
    private String status; // PRESCRIBED | BILLED
    private LocalDateTime createdAt;
    private List<PrescribedItemDTO> items;
}
