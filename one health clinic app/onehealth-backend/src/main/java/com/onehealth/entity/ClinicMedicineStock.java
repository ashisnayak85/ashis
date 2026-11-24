package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One row per (clinic, medicine) - the clinic's chosen margin for that medicine
 * plus a rolled-up quantity-on-hand (sum of all its StockBatch rows still
 * carrying stock). Kept as a maintained rollup (rather than a live SUM query
 * every time) so a low-stock check is a cheap read; every stock-affecting
 * operation (add batch, dispense) updates it in the same transaction.
 *
 * `marginPercent` is set ONLY by the owner (MedicineService.updateMargin) -
 * never by the clinic admin. This is a deliberate control: letting a branch
 * set its own markup would let it silently overcharge patients (or quietly
 * undercharge in exchange for cash off-book) without the quantity-based
 * shrinkage report ever catching it, since quantities would still reconcile.
 * It only applies freely to non-scheduled medicines - see
 * Medicine.scheduledDrug javadoc. For a scheduled drug this is capped at the
 * legal 16% DPCO limit, enforced in MedicineService.
 */
@Entity
@Table(name = "clinic_medicine_stocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clinic_id", "medicine_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicMedicineStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal marginPercent = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantityOnHand = 0;

    // Below this, the dashboard raises a low-stock alert for this clinic/medicine.
    @Builder.Default
    @Column(nullable = false)
    private Integer reorderThreshold = 10;
}
