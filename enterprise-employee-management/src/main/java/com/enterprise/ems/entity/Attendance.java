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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
