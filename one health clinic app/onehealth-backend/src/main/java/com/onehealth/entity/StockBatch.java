package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single dispatch of medicine from the owner/HQ down to one clinic - this is
 * the owner's central purchasing record, not something a clinic buys on its
 * own. Cost price fluctuates dispatch to dispatch (HQ's own supplier deals
 * change), and every dispatch has its own expiry - so profit has to be
 * computed off the batch that was actually dispensed, not off some single
 * "current cost" number for the medicine. Dispensing follows FEFO
 * (First-Expire-First-Out): whichever batch expires soonest is used up
 * first, to minimize wastage - standard pharmacy practice.
 *
 * `costPrice` and `mrp` here are PER BASE UNIT (e.g. per tablet), even when
 * the owner physically received/entered this batch as a number of packs
 * (e.g. strips) - see `packsReceived`. Stock is always tracked and dispensed
 * in base units so a clinic can sell a single loose tablet, never a whole
 * pack, from a rural counter where a patient may only be able to afford a
 * few tablets rather than the full strip.
 *
 * Centralizing dispatch here (rather than letting each clinic record its own
 * purchases) is what makes reconciliation possible: HQ knows exactly what it
 * sent, the system knows exactly what got sold, so any gap between
 * "dispatched minus sold" and a clinic's physical count is a real signal -
 * shrinkage, miscounting, or theft - that a self-reported purchase model
 * can't catch, since the clinic would be grading its own homework.
 */
@Entity
@Table(name = "stock_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(length = 60)
    private String batchNumber;

    // Cost per base unit (e.g. per tablet) - the actual price HQ paid, never
    // shown to a clinic admin or doctor, only to the owner.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    // Printed Maximum Retail Price per base unit - the legal ceiling on what a
    // patient can be charged for one unit. Manufacturers can revise MRP between
    // production runs (e.g. the annual DPCO/WPI revision), so this is
    // snapshotted per batch rather than stored once on the catalog entry.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    // How many whole packs (e.g. strips) this dispatch was entered as, if the
    // medicine has a packSize - purely for the owner's own audit trail /
    // display ("received 5 strips"). Null when dispatched directly in loose
    // base units (medicine.packSize is null) or a fractional/loose amount.
    @Column(name = "packs_received")
    private Integer packsReceived;

    // Always in base units (e.g. tablets), regardless of how packsReceived
    // was entered - see class javadoc.
    @Column(nullable = false)
    private Integer quantityReceived;

    // Decremented as this batch is dispensed against, in base units; 0 = fully used up.
    @Column(nullable = false)
    private Integer quantityRemaining;

    private LocalDate expiryDate;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    // Which owner-side user recorded this dispatch - an audit trail, so
    // "who sent this stock and when" is always answerable.
    @Column(name = "dispatched_by_owner_id")
    private Long dispatchedByOwnerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.receivedDate == null) this.receivedDate = LocalDate.now();
    }
}
