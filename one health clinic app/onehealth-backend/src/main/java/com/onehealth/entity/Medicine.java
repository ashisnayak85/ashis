package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Org-level medicine catalog entry (name/manufacturer stay the same across every
 * branch - only stock quantity/cost/margin differ per clinic, see
 * ClinicMedicineStock). `isScheduledDrug` matters for pricing: under India's
 * Drug Price Control Order (DPCO), a "scheduled formulation" (on the National
 * List of Essential Medicines) has its retailer margin capped at 16% by law -
 * a clinic cannot legally mark these up further. Non-scheduled medicines have
 * no such cap; the OWNER (not the clinic) sets the margin per branch - see
 * MedicineService.updateMargin.
 *
 * DELIBERATELY has no price field: cost, MRP and margin all live at the
 * dispatch/batch level (StockBatch) instead. A medicine's identity (what it
 * is) shouldn't carry a price, since price is a per-shipment, per-branch
 * commercial decision, not a catalog fact - see StockBatch's class javadoc.
 *
 * `unit` is the smallest unit the medicine is actually dispensed in (e.g.
 * "tablet", "ml", "capsule") - this is what quantities are always counted and
 * sold in, right down to a single loose tablet. `packSize`/`packLabel` are an
 * OPTIONAL convenience for how the medicine is physically received/dispatched
 * in bulk (e.g. a "strip" of 10 tablets, a "box" of 5 vials) - stock itself is
 * always stored and dispensed in the base `unit`, never in packs, so a
 * clinic can always sell a single loose tablet even if it was dispatched a
 * full strip at a time. See StockBatch/DispatchStockRequest for the
 * pack -> base-unit conversion at dispatch time.
 */
@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String genericName;

    @Column(length = 150)
    private String manufacturer;

    // The base/loose unit stock is always counted and dispensed in - e.g.
    // "tablet", "ml", "capsule", "sachet". Never a bulk pack unit.
    @Column(length = 30)
    private String unit;

    @Column(length = 80)
    private String category;

    // Legal cap on retailer margin (16%, per DPCO) applies when true - see class javadoc.
    @Builder.Default
    @Column(name = "is_scheduled_drug", nullable = false)
    private boolean scheduledDrug = false;

    // How many base `unit`s make up one physical pack, e.g. 10 (tablets per strip).
    // Null/1 means this medicine isn't packaged in bulk - it's received and
    // dispensed one `unit` at a time (e.g. a bottle, a tube).
    @Column(name = "pack_size")
    private Integer packSize;

    // Display name for the pack, e.g. "strip", "box", "carton". Only meaningful
    // when packSize is set - purely a label for the dispatch screen; stock
    // quantities themselves are never stored in this unit.
    @Column(name = "pack_label", length = 30)
    private String packLabel;

    @Builder.Default
    private boolean active = true;
}
