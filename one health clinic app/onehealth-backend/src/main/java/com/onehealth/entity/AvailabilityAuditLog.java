package com.onehealth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Who did what, and when, to a doctor's weekly time-period templates - created,
 * deleted, turned on, or turned off. Both the owner and clinic authorities can
 * toggle a time period on/off (e.g. the doctor is unexpectedly unavailable),
 * so a clear "who changed this and when" trail matters for accountability
 * between two different roles touching the same schedule.
 *
 * Deliberately denormalized (doctor/clinic name, day, times, actor name all
 * copied in at the moment of the change) rather than just storing foreign
 * keys - an audit log should read the same way five years from now even if
 * the underlying availability row was later deleted, the doctor reassigned,
 * or the staff member who made the change has since left and their account
 * was removed. A log that goes blank when the thing it's describing is gone
 * defeats the purpose of having a log at all.
 */
@Entity
@Table(name = "availability_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // Soft reference only (not a hard FK) - the availability row this refers to
    // may later be deleted; the log should still make sense on its own.
    @Column(name = "availability_id")
    private Long availabilityId;

    private Long doctorId;
    @Column(nullable = false, length = 150)
    private String doctorName;

    private Long clinicId;
    @Column(nullable = false, length = 150)
    private String clinicName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AvailabilityAction action;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    @Column(name = "performed_by_name", nullable = false, length = 150)
    private String performedByName;

    // "OWNER", "CLINIC_ADMIN", or "DOCTOR" - denormalized as a plain string
    // rather than the User.Role enum, since this table should never break if
    // the role enum itself is ever refactored.
    @Column(name = "performed_by_role", nullable = false, length = 20)
    private String performedByRole;

    // e.g. a resync summary ("3 slots cancelled, 1 booked appointment flagged")
    @Column(length = 500)
    private String note;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        this.performedAt = LocalDateTime.now();
    }
}
