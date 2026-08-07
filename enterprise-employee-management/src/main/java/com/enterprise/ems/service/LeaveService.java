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

    List<LeaveDTO> getPendingLeaves();
}
