package com.enterprise.ems.service;

import com.enterprise.ems.dto.LeaveDTO;
import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeaveService {

    LeaveDTO applyLeave(LeaveDTO dto);

    LeaveDTO approveLeave(Long id, String approvedBy);

    LeaveDTO rejectLeave(Long id, String approvedBy);

    LeaveDTO getById(Long id);

    PageResponse<LeaveDTO> getByEmployee(Long employeeId, Pageable pageable);

    // Self-service history for the logged-in employee; status is optional (null = all).
    PageResponse<LeaveDTO> getMyLeaves(Long employeeId, String status, Pageable pageable);

    List<LeaveDTO> getPendingLeaves();
}
