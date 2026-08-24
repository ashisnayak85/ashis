package com.doctorapp.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A physical clinic location. Owned/managed by a {@link ClinicAdmin}, NOT by a
 * doctor - a clinic is an independent entity that many doctors can be associated
 * with (see {@link DoctorClinicAssociation}), and one doctor can be associated
 * with many clinics. Previously this entity had a direct doctor_id FK, which
 * modeled "a doctor's practice location" rather than a real shared clinic; that
 * made it impossible to represent multiple doctors sharing one clinic.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_admin_id", nullable = false)
    private ClinicAdmin clinicAdmin;

    @Column(nullable = false, length = 150)
    private String clinicName;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String pincode;

    @Column(length = 20)
    private String phone;

    // Mirrors Doctor.verified - an admin should sanity-check a clinic before it can
    // accept doctor associations / show up in public search, same reasoning as
    // doctor verification (keeps junk/duplicate listings out of patient search).
    @Builder.Default
    private boolean verified = false;

    @Builder.Default
    private boolean active = true;
}
