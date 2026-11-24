package com.enterprise.ems.service;

import com.enterprise.ems.dto.LeaveDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LeaveService {

    LeaveDTO applyLeave(LeaveDTO dto);

    LeaveDTO approveLeave(Long id, String approvedBy);

    LeaveDTO rejectLeave(Long id, String approvedBy);

    LeaveDTO getById(Long id);

    PageResponse<LeaveDTO> getByEmployee(Long employeeId, Pageable pageable);

    // Self-service history for the logged-in employee. All filters optional (null = no filter).
    // Date range is an overlap check: leaves active at any point within [from, to].
    PageResponse<LeaveDTO> getMyLeaves(Long employeeId, String status, LocalDate from, LocalDate to, Pageable pageable);

    List<LeaveDTO> getPendingLeaves();

    // Admin/manager search across all employees. employeeId is null here (that's
    // what distinguishes this from getMyLeaves) - every other filter is optional.
    PageResponse<LeaveDTO> searchLeaves(String status, LocalDate from, LocalDate to, String employeeName, Pageable pageable);

    // .xlsx export of the full filtered result set (no pagination) - same
    // filters as searchLeaves/getMyLeaves. employeeId null = admin export
    // across everyone; non-null = one employee's own export.
    byte[] exportLeaves(Long employeeId, String status, LocalDate from, LocalDate to, String employeeName);
}
