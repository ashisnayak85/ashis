package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.ResignationApprovalDTO;
import com.enterprise.ems.dto.ResignationAssetDTO;
import com.enterprise.ems.dto.ResignationClearanceDTO;
import com.enterprise.ems.dto.ResignationDTO;
import com.enterprise.ems.entity.Resignation;
import com.enterprise.ems.entity.ResignationApproval;
import com.enterprise.ems.entity.ResignationAsset;
import com.enterprise.ems.entity.ResignationClearance;
import org.springframework.stereotype.Component;

@Component
public class ResignationMapper {

    public ResignationDTO toDTO(Resignation r) {
        return ResignationDTO.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeName(r.getEmployee().getFullName())
                .employeeCode(r.getEmployee().getEmployeeCode())
                .reason(r.getReason())
                .requestedOn(r.getRequestedOn())
                .noticePeriodDays(r.getNoticePeriodDays())
                .expectedLastWorkingDay(r.getExpectedLastWorkingDay())
                .actualLastWorkingDay(r.getActualLastWorkingDay())
                .overrideReason(r.getOverrideReason())
                .status(r.getStatus())
                .approvals(r.getApprovals().stream().map(this::toDTO).toList())
                .clearances(r.getClearances().stream().map(this::toDTO).toList())
                .assets(r.getAssets().stream().map(this::toDTO).toList())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public ResignationApprovalDTO toDTO(ResignationApproval a) {
        return ResignationApprovalDTO.builder()
                .id(a.getId())
                .stageId(a.getStage().getId())
                .stageName(a.getStageName())
                .sequenceOrder(a.getSequenceOrder())
                .approverEmployeeId(a.getApproverEmployee().getId())
                .approverName(a.getApproverEmployee().getFullName())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .actedAt(a.getActedAt())
                .build();
    }

    public ResignationClearanceDTO toDTO(ResignationClearance c) {
        return ResignationClearanceDTO.builder()
                .id(c.getId())
                .departmentId(c.getDepartment().getId())
                .departmentName(c.getDepartment().getName())
                .status(c.getStatus())
                .clearedByEmployeeId(c.getClearedBy() != null ? c.getClearedBy().getId() : null)
                .clearedByName(c.getClearedBy() != null ? c.getClearedBy().getFullName() : null)
                .remarks(c.getRemarks())
                .clearedAt(c.getClearedAt())
                .build();
    }

    public ResignationAssetDTO toDTO(ResignationAsset a) {
        return ResignationAssetDTO.builder()
                .id(a.getId())
                .assetDescription(a.getAssetDescription())
                .returned(a.getReturned())
                .remarks(a.getRemarks())
                .returnedAt(a.getReturnedAt())
                .build();
    }
}
