package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * `source` is the key addition vs. the marketplace project's Appointment: it
 * distinguishes a patient who booked through the app (ONLINE) from a walk-in
 * booked at the front desk by clinic staff (WALK_IN). The owner dashboard needs
 * this split constantly ("how many of our visits are walk-ins vs. app
 * bookings, per branch").
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private AppointmentSlot slot;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingSource source;

    // The user (patient self, or clinic-admin User.id) who created the booking -
    // audit trail, separate from `source` which just says the channel/category.
    @Column(name = "booked_by_user_id", nullable = false)
    private Long bookedByUserId;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AppointmentStatus {
        BOOKED, CANCELLED, COMPLETED, NO_SHOW
    }

    public enum BookingSource {
        ONLINE, WALK_IN
    }

    public enum PaymentStatus {
        PENDING, PAID, NOT_REQUIRED, REFUNDED
    }
}
