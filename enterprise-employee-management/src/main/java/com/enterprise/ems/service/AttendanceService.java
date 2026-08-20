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

    // Employee punching their OWN attendance in, right now. Always today, always
    // the caller, always the server's clock - never a time the client sent.
    // Throws if they've already punched in today. faceVerified must only ever
    // be a value the CALLER computed itself via a real FaceRecognitionService
    // check (or false when the feature is off) - never a value taken from the
    // client's request body, or a user could just claim "verified: true".
    AttendanceDTO punchIn(Long employeeId, String remarks, boolean faceVerified);

    // Employee punching their OWN attendance out, right now. Throws if they
    // haven't punched in yet today, or have already punched out. Same
    // faceVerified caution as punchIn applies here.
    AttendanceDTO punchOut(Long employeeId, String remarks, boolean faceVerified);

    // Today's attendance row for this employee, or null if they haven't punched
    // in yet - lets the frontend decide whether to show "Punch In" or "Punch Out".
    AttendanceDTO getTodayStatus(Long employeeId);

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
