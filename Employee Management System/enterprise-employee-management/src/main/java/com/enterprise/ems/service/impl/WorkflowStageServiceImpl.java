package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.WorkflowStageDTO;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.PrivilegeModule;
import com.enterprise.ems.entity.WorkflowRole;
import com.enterprise.ems.entity.WorkflowStage;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.WorkflowRoleMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.PrivilegeModuleRepository;
import com.enterprise.ems.repository.WorkflowRoleRepository;
import com.enterprise.ems.repository.WorkflowStageRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.WorkflowStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/*
 * PURPOSE: Implements admin CRUD for a module's approval chain. Renamed and
 * generalized from ResignationApprovalStageServiceImpl - see
 * WorkflowStageService javadoc. Validation logic is unchanged from the
 * resignation-only version; only the module scoping is new.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowStageServiceImpl implements WorkflowStageService {

    private static final List<String> VALID_APPROVER_TYPES = List.of(
            AppConstants.APPROVER_TYPE_REPORTING_MANAGER,
            AppConstants.APPROVER_TYPE_ROLE_IN_DEPARTMENT,
            AppConstants.APPROVER_TYPE_SPECIFIC_ROLE,
            AppConstants.APPROVER_TYPE_SPECIFIC_USER);

    private static final List<String> VALID_DEPARTMENT_SCOPES = List.of("EMPLOYEE_DEPARTMENT", "FIXED_DEPARTMENT");

    private final WorkflowStageRepository stageRepository;
    private final PrivilegeModuleRepository privilegeModuleRepository;
    private final WorkflowRoleRepository workflowRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowRoleMapper mapper;
    private final AuditService auditService;

    @Override
    public WorkflowStageDTO create(WorkflowStageDTO request) {
        PrivilegeModule module = resolveModuleOrThrow(request.getPrivilegeModuleId());
        validateCommon(request);
        if (stageRepository.existsByPrivilegeModule_IdAndSequenceOrder(module.getId(), request.getSequenceOrder())) {
            throw new BusinessException("Sequence order " + request.getSequenceOrder()
                    + " is already used by another stage in " + module.getName() + " - reorder instead");
        }

        WorkflowStage stage = WorkflowStage.builder()
                .privilegeModule(module)
                .name(request.getName().trim())
                .sequenceOrder(request.getSequenceOrder())
                .approverType(request.getApproverType())
                .workflowRole(resolveWorkflowRoleOrNull(request.getWorkflowRoleId()))
                .departmentScope(request.getDepartmentScope())
                .fixedDepartment(resolveDepartmentOrNull(request.getFixedDepartmentId()))
                .specificEmployee(resolveEmployeeOrNull(request.getSpecificEmployeeId()))
                .build();
        stage = stageRepository.save(stage);

        auditService.log("CREATE", "WorkflowStage", stage.getId(),
                module.getCode() + " approval stage created: " + stage.getName() + " (seq " + stage.getSequenceOrder() + ")");
        return mapper.toDTO(stage);
    }

    @Override
    public WorkflowStageDTO update(Long id, WorkflowStageDTO request) {
        WorkflowStage stage = getOrThrow(id);
        validateCommon(request);

        // Module reassignment isn't supported through update - a stage
        // belongs to the module it was created for. Only sequence/approver
        // details can change.
        Long moduleId = stage.getPrivilegeModule().getId();
        if (!request.getSequenceOrder().equals(stage.getSequenceOrder())
                && stageRepository.existsByPrivilegeModule_IdAndSequenceOrderAndIdNot(moduleId, request.getSequenceOrder(), id)) {
            throw new BusinessException("Sequence order " + request.getSequenceOrder() + " is already used by another stage in this module - reorder instead");
        }

        stage.setName(request.getName().trim());
        stage.setSequenceOrder(request.getSequenceOrder());
        stage.setApproverType(request.getApproverType());
        stage.setWorkflowRole(resolveWorkflowRoleOrNull(request.getWorkflowRoleId()));
        stage.setDepartmentScope(request.getDepartmentScope());
        stage.setFixedDepartment(resolveDepartmentOrNull(request.getFixedDepartmentId()));
        stage.setSpecificEmployee(resolveEmployeeOrNull(request.getSpecificEmployeeId()));
        stageRepository.save(stage);

        auditService.log("UPDATE", "WorkflowStage", stage.getId(), "Approval stage updated: " + stage.getName());
        return mapper.toDTO(stage);
    }

    @Override
    public WorkflowStageDTO deactivate(Long id) {
        WorkflowStage stage = getOrThrow(id);
        stage.setActive(false);
        stageRepository.save(stage);
        auditService.log("DEACTIVATE", "WorkflowStage", stage.getId(), "Approval stage deactivated: " + stage.getName());
        return mapper.toDTO(stage);
    }

    @Override
    public WorkflowStageDTO reactivate(Long id) {
        WorkflowStage stage = getOrThrow(id);
        Long moduleId = stage.getPrivilegeModule().getId();
        if (stageRepository.existsByPrivilegeModule_IdAndSequenceOrderAndIdNot(moduleId, stage.getSequenceOrder(), id)) {
            throw new BusinessException("Cannot reactivate: sequence order " + stage.getSequenceOrder()
                    + " is now used by another stage in this module - update this stage's sequence order first");
        }
        stage.setActive(true);
        stageRepository.save(stage);
        auditService.log("REACTIVATE", "WorkflowStage", stage.getId(), "Approval stage reactivated: " + stage.getName());
        return mapper.toDTO(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStageDTO> getForModule(Long privilegeModuleId) {
        resolveModuleOrThrow(privilegeModuleId); // 404 fast if the module id is bogus
        return stageRepository.findByPrivilegeModule_IdOrderBySequenceOrderAsc(privilegeModuleId)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<WorkflowStageDTO> reorder(Long privilegeModuleId, List<Long> orderedStageIds) {
        if (orderedStageIds == null || orderedStageIds.isEmpty()) {
            throw new BusinessException("At least one stage id is required to reorder");
        }
        resolveModuleOrThrow(privilegeModuleId);

        List<WorkflowStage> stages = new ArrayList<>();
        for (Long id : orderedStageIds) {
            WorkflowStage stage = getOrThrow(id);
            if (!stage.getPrivilegeModule().getId().equals(privilegeModuleId)) {
                throw new BusinessException("Stage " + id + " does not belong to the specified module");
            }
            stages.add(stage);
        }

        // Same two-phase temp-offset trick as before, now scoped per module
        // (sequence_order uniqueness is now per-module, but the same
        // mid-swap collision risk applies within a module).
        for (int i = 0; i < stages.size(); i++) {
            stages.get(i).setSequenceOrder(-(i + 1));
        }
        stageRepository.saveAllAndFlush(stages);

        for (int i = 0; i < stages.size(); i++) {
            stages.get(i).setSequenceOrder(i + 1);
        }
        stageRepository.saveAllAndFlush(stages);

        auditService.log("REORDER", "WorkflowStage", null,
                "Approval chain reordered for module " + privilegeModuleId + ": " + orderedStageIds);
        return stages.stream().map(mapper::toDTO).toList();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void validateCommon(WorkflowStageDTO request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("Stage name is required");
        }
        if (request.getSequenceOrder() == null || request.getSequenceOrder() < 1) {
            throw new BusinessException("Sequence order is required and must be 1 or greater");
        }
        if (request.getApproverType() == null || !VALID_APPROVER_TYPES.contains(request.getApproverType())) {
            throw new BusinessException("approverType must be one of " + VALID_APPROVER_TYPES);
        }

        switch (request.getApproverType()) {
            case AppConstants.APPROVER_TYPE_ROLE_IN_DEPARTMENT -> {
                if (request.getWorkflowRoleId() == null) {
                    throw new BusinessException("workflowRoleId is required when approverType = ROLE_IN_DEPARTMENT");
                }
                if (request.getDepartmentScope() == null || !VALID_DEPARTMENT_SCOPES.contains(request.getDepartmentScope())) {
                    throw new BusinessException("departmentScope must be one of " + VALID_DEPARTMENT_SCOPES + " when approverType = ROLE_IN_DEPARTMENT");
                }
                if ("FIXED_DEPARTMENT".equals(request.getDepartmentScope()) && request.getFixedDepartmentId() == null) {
                    throw new BusinessException("fixedDepartmentId is required when departmentScope = FIXED_DEPARTMENT");
                }
            }
            case AppConstants.APPROVER_TYPE_SPECIFIC_ROLE -> {
                if (request.getWorkflowRoleId() == null) {
                    throw new BusinessException("workflowRoleId is required when approverType = SPECIFIC_ROLE");
                }
            }
            case AppConstants.APPROVER_TYPE_SPECIFIC_USER -> {
                if (request.getSpecificEmployeeId() == null) {
                    throw new BusinessException("specificEmployeeId is required when approverType = SPECIFIC_USER");
                }
            }
            default -> { /* REPORTING_MANAGER needs none of the above */ }
        }
    }

    private PrivilegeModule resolveModuleOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("privilegeModuleId is required");
        }
        PrivilegeModule module = privilegeModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Privilege module not found: " + id));
        if (!module.getActive()) {
            throw new BusinessException("Cannot configure stages for an inactive module (" + module.getName() + ")");
        }
        return module;
    }

    private WorkflowRole resolveWorkflowRoleOrNull(Long id) {
        if (id == null) return null;
        WorkflowRole role = workflowRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow role not found: " + id));
        if (!role.getActive()) {
            throw new BusinessException("Cannot use an inactive workflow role (" + role.getName() + ") on an approval stage");
        }
        return role;
    }

    private Department resolveDepartmentOrNull(Long id) {
        if (id == null) return null;
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Employee resolveEmployeeOrNull(Long id) {
        if (id == null) return null;
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private WorkflowStage getOrThrow(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow stage not found: " + id));
    }
}
