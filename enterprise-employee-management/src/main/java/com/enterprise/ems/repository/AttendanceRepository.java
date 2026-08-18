package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    Page<Attendance> findByEmployeeId(Long employeeId, Pageable pageable);

    List<Attendance> findByAttendanceDate(LocalDate date);

    long countByAttendanceDateAndStatus(LocalDate date, String status);

    // Powers the "my dashboard" month summary (present/absent/on-leave day counts).
    long countByEmployeeIdAndAttendanceDateBetweenAndStatus(
            Long employeeId, LocalDate startDate, LocalDate endDate, String status);
}
