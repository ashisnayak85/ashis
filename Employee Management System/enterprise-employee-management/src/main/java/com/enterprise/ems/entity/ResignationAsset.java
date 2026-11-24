package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: Asset return line items surfaced during no-dues. Deliberately
 * lightweight - a free-text description per item, not a full asset-tracking
 * module (no asset master, no assignment history). Fine for v1 since
 * resignation is the only place assets are referenced today; if a broader
 * asset-management feature gets built later, swap assetDescription for a
 * FK to a real Asset entity without changing anything else here.
 * TABLE: resignation_asset
 */
@Entity
@Table(name = "resignation_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resignation_id", nullable = false)
    private Resignation resignation;

    @Column(name = "asset_description", nullable = false, length = 255)
    private String assetDescription; // e.g. "Dell Latitude 5420 laptop, tag EMS-IT-0231"

    @Column(name = "returned", nullable = false)
    @Builder.Default
    private Boolean returned = false;

    @Column(name = "remarks", length = 500)
    private String remarks; // e.g. condition notes, deduction if not returned

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
}
