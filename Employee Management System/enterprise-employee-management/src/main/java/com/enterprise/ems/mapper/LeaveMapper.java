package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {

    public LeaveDTO toDTO(LeaveMaster entity) {
        if (entity == null) return null;
        return LeaveDTO.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .employeeName(entity.getEmployee().getFullName())
                .leaveType(entity.getLeaveType())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .build();
    }

    public LeaveMaster toEntity(LeaveDTO dto, Employee employee) {
        return LeaveMaster.builder()
                .id(dto.getId())
                .employee(employee)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(dto.getStatus() != null ? dto.getStatus() : "PENDING")
                .approvedBy(dto.getApprovedBy())
                .build();
    }
}
