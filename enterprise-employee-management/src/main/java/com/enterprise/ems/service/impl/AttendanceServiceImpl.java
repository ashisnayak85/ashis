package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.AttendanceDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Attendance;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.AttendanceMapper;
import com.enterprise.ems.repository.AttendanceRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.AttendanceService;
import com.enterprise.ems.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        attendanceRepository.findByEmployeeIdAndAttendanceDate(dto.getEmployeeId(), dto.getAttendanceDate())
                .ifPresent(a -> {
                    throw new BusinessException("Attendance already marked for this date");
                });

        Attendance saved = attendanceRepository.save(attendanceMapper.toEntity(dto, employee));
        auditService.log("CREATE", "Attendance", saved.getId(), "Marked attendance for employee: " + employee.getEmployeeCode());
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
