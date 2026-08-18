package com.enterprise.ems.service;

import com.enterprise.ems.dto.AttendanceDTO;
import com.enterprise.ems.dto.BiometricPunchDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    // Admin/Manager marking or correcting attendance for any employee, any date.
    // Persisted with source=ADMIN.
    AttendanceDTO markAttendance(AttendanceDTO dto);

    // Employee marking their OWN attendance. employeeId on the incoming dto is
    // ignored - always resolved from the session - and the date is forced to
    // today (no backdating/future-dating from self-service). source=SELF.
    AttendanceDTO markSelfAttendance(Long employeeId, AttendanceDTO dto);

    // Ingestion endpoint for a biometric device push. Finds-or-creates the day's
    // attendance row for the employee and fills in check-in or check-out based on
    // punchType. source=BIOMETRIC.
    AttendanceDTO recordBiometricPunch(BiometricPunchDTO dto);

    AttendanceDTO update(Long id, AttendanceDTO dto);

    AttendanceDTO getById(Long id);

    PageResponse<AttendanceDTO> getByEmployee(Long employeeId, Pageable pageable);

    // Combined filter: every argument is optional (nullable) so the UI can mix
    // and match - date range only, employee only, status only, or all together.
    PageResponse<AttendanceDTO> search(Long employeeId, LocalDate startDate, LocalDate endDate, String status, Pageable pageable);

    List<AttendanceDTO> getByDate(LocalDate date);

    long countPresentToday();
}
