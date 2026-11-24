package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One BILLED (actually dispensed and charged) line item - created only when
 * the clinic admin creates the bill (see Prescription.PrescriptionStatus),
 * not when the doctor prescribes. `quantity` here is what the admin actually
 * chose to dispense, which may be less than PrescribedItem.prescribedQuantity
 * (e.g. patient could only afford 6 of the 10 prescribed tablets - stock is
 * always tracked/sold in base units, never whole packs, precisely so this is
 * possible).
 *
 * This is the actual sales/profit ledger the owner's dashboard reports
 * against. costPriceAtSale/sellingPriceAtSale/profitAmount are SNAPSHOTTED at
 * the moment of billing rather than joined live from ClinicMedicineStock - if
 * the clinic's margin (set by the owner) changes next month, every past
 * month's profit report must stay exactly what it was, not silently
 * recalculate. costPriceAtSale and profitAmount are OWNER-ONLY data - never
 * returned to a clinic admin or doctor; see MedicineService/BillReceiptDTO.
 */
@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPriceAtSale;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPriceAtSale;

    // (sellingPriceAtSale - costPriceAtSale) * quantity - stored, not recomputed,
    // for the same "history shouldn't silently change" reason as the prices above.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal profitAmount;

    @Column(length = 100)
    private String dosageInstructions;

    // When this line was actually BILLED (not when it was prescribed) - the
    // dashboard's date-range reporting filters on this, since that's when the
    // sale/payment actually happened.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
