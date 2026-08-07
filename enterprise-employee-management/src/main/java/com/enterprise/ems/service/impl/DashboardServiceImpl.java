package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.DashboardStats;
import com.enterprise.ems.repository.AttendanceRepository;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.LeaveRepository;
import com.enterprise.ems.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;

    @Override
    public DashboardStats getStats() {
        return DashboardStats.builder()
                .totalEmployees(employeeRepository.count())
                .totalDepartments(departmentRepository.count())
                .presentToday(attendanceRepository.countByAttendanceDateAndStatus(
                        LocalDate.now(), AppConstants.ATTENDANCE_PRESENT))
                .pendingLeaves(leaveRepository.countByStatus(AppConstants.LEAVE_PENDING))
                .build();
    }
}
