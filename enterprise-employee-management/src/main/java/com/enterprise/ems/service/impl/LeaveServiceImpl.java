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
import com.enterprise.ems.repository.spec.LeaveSpecifications;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.LeaveService;
import com.enterprise.ems.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        validateDateOrder(dto.getStartDate(), dto.getEndDate(), "End date cannot be before start date");
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
    public PageResponse<LeaveDTO> getMyLeaves(Long employeeId, String status, LocalDate from, LocalDate to, Pageable pageable) {
        validateDateOrder(from, to, "\"To\" date cannot be before \"From\" date");

        Specification<LeaveMaster> spec = Specification
                .where(LeaveSpecifications.forEmployee(employeeId))
                .and(LeaveSpecifications.hasStatus(status))
                .and(LeaveSpecifications.dateRangeOverlaps(from, to));

        return toPageResponse(leaveRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveDTO> getPendingLeaves() {
        return leaveRepository.findByStatus(AppConstants.LEAVE_PENDING).stream()
                .map(leaveMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveDTO> searchLeaves(String status, LocalDate from, LocalDate to, String employeeName, Pageable pageable) {
        validateDateOrder(from, to, "\"To\" date cannot be before \"From\" date");

        Specification<LeaveMaster> spec = Specification
                .where(LeaveSpecifications.hasStatus(status))
                .and(LeaveSpecifications.dateRangeOverlaps(from, to))
                .and(LeaveSpecifications.employeeNameLike(employeeName));

        return toPageResponse(leaveRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportLeaves(Long employeeId, String status, LocalDate from, LocalDate to, String employeeName) {
        validateDateOrder(from, to, "\"To\" date cannot be before \"From\" date");

        Specification<LeaveMaster> spec = Specification
                .where(LeaveSpecifications.forEmployee(employeeId))
                .and(LeaveSpecifications.hasStatus(status))
                .and(LeaveSpecifications.dateRangeOverlaps(from, to))
                .and(LeaveSpecifications.employeeNameLike(employeeName));

        List<LeaveMaster> leaves = leaveRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startDate"));

        List<String> headers = employeeId == null
                ? List.of("Employee", "Leave Type", "Start Date", "End Date", "Reason", "Status", "Approved By")
                : List.of("Leave Type", "Start Date", "End Date", "Reason", "Status", "Approved By");

        return ExcelExportUtil.toXlsx("Leave Requests", headers, leaves, leave -> employeeId == null
                ? List.of(
                        leave.getEmployee().getFullName(),
                        nullToEmpty(leave.getLeaveType()),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        nullToEmpty(leave.getReason()),
                        nullToEmpty(leave.getStatus()),
                        nullToEmpty(leave.getApprovedBy()))
                : List.of(
                        nullToEmpty(leave.getLeaveType()),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        nullToEmpty(leave.getReason()),
                        nullToEmpty(leave.getStatus()),
                        nullToEmpty(leave.getApprovedBy())));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void validateDateOrder(LocalDate from, LocalDate to, String message) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessException(message);
        }
    }

    private PageResponse<LeaveDTO> toPageResponse(Page<LeaveMaster> page) {
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
}
