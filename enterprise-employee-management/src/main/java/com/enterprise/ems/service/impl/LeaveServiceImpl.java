package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.LeaveDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.LeaveMaster;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.LeaveMapper;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.LeaveRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;
    private final AuditService auditService;

    @Override
    public LeaveDTO applyLeave(LeaveDTO dto) {
        if (dto.getEmployeeId() == null) {
            throw new BusinessException("Employee is required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("End date cannot be before start date");
        }
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        LeaveMaster saved = leaveRepository.save(leaveMapper.toEntity(dto, employee));
        auditService.log("CREATE", "Leave", saved.getId(), "Leave applied by: " + employee.getEmployeeCode());
        return leaveMapper.toDTO(saved);
    }

    @Override
    public LeaveDTO approveLeave(Long id, String approvedBy) {
        LeaveMaster leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + id));
        leave.setStatus(AppConstants.LEAVE_APPROVED);
        leave.setApprovedBy(approvedBy);
        auditService.log("UPDATE", "Leave", id, "Leave approved by: " + approvedBy);
        return leaveMapper.toDTO(leaveRepository.save(leave));
    }

    @Override
    public LeaveDTO rejectLeave(Long id, String approvedBy) {
        LeaveMaster leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + id));
        leave.setStatus(AppConstants.LEAVE_REJECTED);
        leave.setApprovedBy(approvedBy);
        auditService.log("UPDATE", "Leave", id, "Leave rejected by: " + approvedBy);
        return leaveMapper.toDTO(leaveRepository.save(leave));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveDTO getById(Long id) {
        return leaveMapper.toDTO(leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveDTO> getByEmployee(Long employeeId, Pageable pageable) {
        Page<LeaveMaster> page = leaveRepository.findByEmployeeId(employeeId, pageable);
        return PageResponse.<LeaveDTO>builder()
                .content(page.getContent().stream().map(leaveMapper::toDTO).toList())
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
    public PageResponse<LeaveDTO> getMyLeaves(Long employeeId, String status, Pageable pageable) {
        Page<LeaveMaster> page = (status == null || status.isBlank())
                ? leaveRepository.findByEmployeeId(employeeId, pageable)
                : leaveRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        return PageResponse.<LeaveDTO>builder()
                .content(page.getContent().stream().map(leaveMapper::toDTO).toList())
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
    public List<LeaveDTO> getPendingLeaves() {
        return leaveRepository.findByStatus(AppConstants.LEAVE_PENDING).stream()
                .map(leaveMapper::toDTO).toList();
    }
}
