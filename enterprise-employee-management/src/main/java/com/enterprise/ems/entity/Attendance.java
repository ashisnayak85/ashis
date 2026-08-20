package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/*
 * PURPOSE: Daily attendance tracking
 * TABLE: attendance
 * WHY EXISTS: HR compliance, payroll, reporting
 */
@Entity
@Table(name = "attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "attendance_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PRESENT, ABSENT, HALF_DAY, ON_LEAVE

    // Who/what recorded this punch: SELF (employee marked their own), ADMIN
    // (HR/manager marked or corrected it), BIOMETRIC (pushed by the attendance
    // machine). Defaults to ADMIN for rows created before this column existed.
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private String source = "ADMIN";

    @Column(name = "remarks", length = 255)
    private String remarks;

    // True only when this SELF punch was matched against the employee's
    // enrolled face by FaceRecognitionService right before saving. Stays false
    // for ADMIN/BIOMETRIC rows and for SELF rows recorded while the feature is
    // disabled - lets HR reports/filters distinguish verified self-punches
    // from unverified ones instead of trusting the source column alone.
    @Column(name = "face_verified", nullable = false)
    @Builder.Default
    private boolean faceVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
