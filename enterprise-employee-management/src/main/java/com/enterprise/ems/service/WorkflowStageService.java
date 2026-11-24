package com.enterprise.ems.service;

import com.enterprise.ems.dto.WorkflowStageDTO;

import java.util.List;

/*
 * PURPOSE: Admin-facing CRUD for a module's approval chain (WorkflowStage).
 * Renamed/generalized from ResignationApprovalStageService - every
 * operation now takes/returns a privilegeModuleId so the SAME service (and
 * the SAME admin screen) covers Resignation today and any future module
 * without new code. See PrivilegeModule / WorkflowStage entity javadocs.
 */
public interface WorkflowStageService {

    WorkflowStageDTO create(WorkflowStageDTO request);

    WorkflowStageDTO update(Long id, WorkflowStageDTO request);

    WorkflowStageDTO deactivate(Long id);

    WorkflowStageDTO reactivate(Long id);

    List<WorkflowStageDTO> getForModule(Long privilegeModuleId);

    // Bulk re-sequencing WITHIN one module - takes the full ordered list of
    // stage ids for that module and rewrites sequenceOrder to match.
    List<WorkflowStageDTO> reorder(Long privilegeModuleId, List<Long> orderedStageIds);
}
