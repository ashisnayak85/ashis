package com.enterprise.ems.repository;

import com.enterprise.ems.entity.WorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

    // The resolved chain for one module, in order - used when a workflow
    // instance of that module is submitted (e.g. ResignationServiceImpl
    // reads this filtered to the RESIGNATION module code).
    List<WorkflowStage> findByPrivilegeModule_CodeAndActiveTrueOrderBySequenceOrderAsc(String moduleCode);

    // Admin CRUD - full list for one module regardless of active flag.
    List<WorkflowStage> findByPrivilegeModule_IdOrderBySequenceOrderAsc(Long privilegeModuleId);

    boolean existsByPrivilegeModule_IdAndSequenceOrder(Long privilegeModuleId, Integer sequenceOrder);

    boolean existsByPrivilegeModule_IdAndSequenceOrderAndIdNot(Long privilegeModuleId, Integer sequenceOrder, Long id);

    boolean existsByWorkflowRoleId(Long workflowRoleId);
}
