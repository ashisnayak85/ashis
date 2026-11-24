package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One line of what the DOCTOR prescribed - medicine + quantity + dosage,
 * nothing else. Deliberately carries no price/cost: the doctor decides what
 * is clinically needed, not what it costs or how much gets charged. The
 * clinic admin later decides the actual BILLED quantity (which may be less,
 * e.g. a patient can only afford part of this) when creating the bill - see
 * PrescriptionItem for that priced, dispensed record.
 */
@Entity
@Table(name = "prescribed_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescribedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // How much the doctor recommended - the ceiling the clinic admin can bill
    // against, not necessarily what actually gets sold.
    @Column(name = "prescribed_quantity", nullable = false)
    private Integer prescribedQuantity;

    @Column(length = 100)
    private String dosageInstructions;
}
