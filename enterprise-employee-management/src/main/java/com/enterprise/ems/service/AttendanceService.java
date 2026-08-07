package com.enterprise.ems.service;

import com.enterprise.ems.dto.AttendanceDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceDTO markAttendance(AttendanceDTO dto);

    AttendanceDTO update(Long id, AttendanceDTO dto);

    AttendanceDTO getById(Long id);

    PageResponse<AttendanceDTO> getByEmployee(Long employeeId, Pageable pageable);

    List<AttendanceDTO> getByDate(LocalDate date);

    long countPresentToday();
}
