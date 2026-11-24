package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A single real, bookable time slot for a doctor at a clinic on a specific date.
 * Generated from DoctorAvailability templates. The unique constraint is a
 * DB-level safety net against two patients locking the same slot concurrently;
 * the actual "doctor can't be double-booked across branches" rule is enforced
 * earlier, at the DoctorAvailability template level (templates can't overlap in
 * time across any of the org's clinics), so slots generated from non-overlapping
 * templates can never collide in the first place.
 */
@Entity
@Table(
    name = "appointment_slots",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_clinic_date_start",
        columnNames = {"doctor_id", "clinic_id", "slot_date", "start_time"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    private Long version;

    public enum SlotStatus {
        AVAILABLE, LOCKED, BOOKED, CANCELLED
    }
}
