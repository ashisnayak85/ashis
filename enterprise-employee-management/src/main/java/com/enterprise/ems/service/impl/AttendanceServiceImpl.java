package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.AttendanceDTO;
import com.enterprise.ems.dto.BiometricPunchDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Attendance;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.AttendanceMapper;
import com.enterprise.ems.repository.AttendanceRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.spec.AttendanceSpecifications;
import com.enterprise.ems.service.AttendanceService;
import com.enterprise.ems.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;
    private final AuditService auditService;

    @Override
    public AttendanceDTO markAttendance(AttendanceDTO dto) {
        return createAttendance(dto, AppConstants.ATTENDANCE_SOURCE_ADMIN, "HR/admin");
    }

    @Override
    public AttendanceDTO punchIn(Long employeeId, String remarks, boolean faceVerified) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseGet(() -> Attendance.builder()
                        .employee(employee)
                        .attendanceDate(today)
                        .status(AppConstants.ATTENDANCE_PRESENT)
                        .build());

        if (attendance.getCheckInTime() != null) {
            throw new BusinessException("Already punched in for today at " + attendance.getCheckInTime());
        }

        // Server clock only - never trust a time value from the client, since a
        // person can freely change their device's date/time to fake an earlier
        // arrival. LocalTime.now() is the server's own clock, not anything sent
        // in the request.
        attendance.setCheckInTime(LocalTime.now());
        attendance.setSource(AppConstants.ATTENDANCE_SOURCE_SELF);
        attendance.setFaceVerified(faceVerified);
        if (remarks != null && !remarks.isBlank()) {
            attendance.setRemarks(remarks);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log("CREATE", "Attendance", saved.getId(),
                "Self punch-in for employee: " + employee.getEmployeeCode() + (faceVerified ? " (face verified)" : ""));
        return attendanceMapper.toDTO(saved);
    }

    @Override
    public AttendanceDTO punchOut(Long employeeId, String remarks, boolean faceVerified) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new BusinessException("You haven't punched in today yet"));

        if (attendance.getCheckInTime() == null) {
            throw new BusinessException("You haven't punched in today yet");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new BusinessException("Already punched out for today at " + attendance.getCheckOutTime());
        }

        attendance.setCheckOutTime(LocalTime.now());
        // Punch-out is verified independently of punch-in - someone could
        // theoretically be verified at 9am and hand their session to someone
        // else by 5pm, so re-checking on the way out matters too. Keep the
        // flag true if EITHER punch-in or punch-out was actually verified,
        // so a disabled-at-punch-out check doesn't erase a real punch-in match.
        attendance.setFaceVerified(attendance.isFaceVerified() || faceVerified);
        if (remarks != null && !remarks.isBlank()) {
            attendance.setRemarks(remarks);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log("UPDATE", "Attendance", saved.getId(),
                "Self punch-out for employee: " + attendance.getEmployee().getEmployeeCode() + (faceVerified ? " (face verified)" : ""));
        return attendanceMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDTO getTodayStatus(Long employeeId) {
        // Deliberately returns null (not a 404) when there's no row yet - "haven't
        // punched in today" is a normal state the frontend uses to show the Punch In
        // button, not an error condition.
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now())
                .map(attendanceMapper::toDTO)
                .orElse(null);
    }

    private AttendanceDTO createAttendance(AttendanceDTO dto, String source, String actorDescription) {
        if (dto.getEmployeeId() == null) {
            throw new BusinessException("Employee is required");
        }
        if (dto.getAttendanceDate() == null) {
            throw new BusinessException("Attendance date is required");
        }
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        attendanceRepository.findByEmployeeIdAndAttendanceDate(dto.getEmployeeId(), dto.getAttendanceDate())
                .ifPresent(a -> {
                    throw new BusinessException("Attendance already marked for this date");
                });

        Attendance entity = attendanceMapper.toEntity(dto, employee);
        entity.setSource(source);
        // Never face-verified - this path is HR/admin manually marking or
        // correcting a record, not a live self-punch with a photo.
        entity.setFaceVerified(false);
        Attendance saved = attendanceRepository.save(entity);
        auditService.log("CREATE", "Attendance", saved.getId(),
                "Marked attendance (" + actorDescription + ") for employee: " + employee.getEmployeeCode());
        return attendanceMapper.toDTO(saved);
    }

    @Override
    public AttendanceDTO recordBiometricPunch(BiometricPunchDTO dto) {
        Employee employee = employeeRepository.findByEmployeeCode(dto.getEmployeeCode())
                .orElseThrow(() -> new ResourceNotFoundException("No employee with code: " + dto.getEmployeeCode()));

        LocalDate date = dto.getTimestamp().toLocalDate();
        boolean isCheckIn = AppConstants.PUNCH_IN.equalsIgnoreCase(dto.getPunchType());

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), date)
                .orElseGet(() -> Attendance.builder()
                        .employee(employee)
                        .attendanceDate(date)
                        .status(AppConstants.ATTENDANCE_PRESENT)
                        .build());

        if (isCheckIn) {
            attendance.setCheckInTime(dto.getTimestamp().toLocalTime());
        } else {
            attendance.setCheckOutTime(dto.getTimestamp().toLocalTime());
        }
        // A punch always means the machine saw the person - this is authoritative,
        // so a biometric punch overwrites a SELF or ADMIN entry for the same day.
        attendance.setSource(AppConstants.ATTENDANCE_SOURCE_BIOMETRIC);
        if (attendance.getStatus() == null) {
            attendance.setStatus(AppConstants.ATTENDANCE_PRESENT);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.log(isCheckIn ? "CREATE" : "UPDATE", "Attendance", saved.getId(),
                "Biometric " + dto.getPunchType() + " punch for employee: " + employee.getEmployeeCode());
        return attendanceMapper.toDTO(saved);
    }

    @Override
    public AttendanceDTO update(Long id, AttendanceDTO dto) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + id));
        attendance.setCheckInTime(dto.getCheckInTime());
        attendance.setCheckOutTime(dto.getCheckOutTime());
        attendance.setStatus(dto.getStatus());
        attendance.setRemarks(dto.getRemarks());
        return attendanceMapper.toDTO(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDTO getById(Long id) {
        return attendanceMapper.toDTO(attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceDTO> getByEmployee(Long employeeId, Pageable pageable) {
        Page<Attendance> page = attendanceRepository.findByEmployeeId(employeeId, pageable);
        return PageResponse.<AttendanceDTO>builder()
                .content(page.getContent().stream().map(attendanceMapper::toDTO).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceDTO> search(Long employeeId, LocalDate startDate, LocalDate endDate, String status, Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be on or before end date");
        }

        Page<Attendance> page = attendanceRepository.findAll(
                AttendanceSpecifications.filterBy(employeeId, startDate, endDate, status), pageable);

        return PageResponse.<AttendanceDTO>builder()
                .content(page.getContent().stream().map(attendanceMapper::toDTO).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // Same filter/scoping as search() above, but the full matching set (unpaged)
    // rendered straight to .xlsx via the shared export util.
    @Override
    @Transactional(readOnly = true)
    public byte[] exportAttendance(Long employeeId, LocalDate startDate, LocalDate endDate, String status) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be on or before end date");
        }

        List<Attendance> records = attendanceRepository.findAll(
                AttendanceSpecifications.filterBy(employeeId, startDate, endDate, status),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "attendanceDate"));

        List<String> headers = List.of("Employee", "Date", "Check In", "Check Out", "Status", "Source", "Remarks");

        return com.enterprise.ems.util.ExcelExportUtil.toXlsx("Attendance", headers, records, r -> List.of(
                r.getEmployee().getFullName(),
                r.getAttendanceDate(),
                r.getCheckInTime() != null ? r.getCheckInTime().toString() : "",
                r.getCheckOutTime() != null ? r.getCheckOutTime().toString() : "",
                r.getStatus(),
                r.getSource(),
                r.getRemarks() != null ? r.getRemarks() : ""));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDTO> getByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date).stream()
                .map(attendanceMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPresentToday() {
        return attendanceRepository.countByAttendanceDateAndStatus(LocalDate.now(), AppConstants.ATTENDANCE_PRESENT);
    }
}
