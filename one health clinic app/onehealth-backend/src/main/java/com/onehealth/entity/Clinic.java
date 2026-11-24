package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One physical branch location belonging to an Organization. Unlike the public
 * marketplace project, there's no "verified" flag here - the owner creates their
 * own branches directly, there's no platform moderation step for a trusted,
 * paying customer's own data.
 */
@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 150)
    private String clinicName;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String pincode;

    @Column(length = 20)
    private String phone;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private boolean active = true;
}
