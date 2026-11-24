package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.PrivilegeModuleDTO;
import com.enterprise.ems.entity.PrivilegeModule;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.WorkflowRoleMapper;
import com.enterprise.ems.repository.PrivilegeModuleRepository;
import com.enterprise.ems.repository.WorkflowStageRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.PrivilegeModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrivilegeModuleServiceImpl implements PrivilegeModuleService {

    private final PrivilegeModuleRepository privilegeModuleRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final WorkflowRoleMapper mapper;
    private final AuditService auditService;

    @Override
    public PrivilegeModuleDTO deactivate(Long id) {
        PrivilegeModule module = getOrThrow(id);
        // Guardrail, same principle as WorkflowRoleServiceImpl: don't let
        // admin silently orphan an active stage chain still configured for
        // this module.
        if (!workflowStageRepository.findByPrivilegeModule_IdOrderBySequenceOrderAsc(id).isEmpty()) {
            throw new BusinessException("This module still has approval stages configured - deactivate/remove those first");
        }
        module.setActive(false);
        privilegeModuleRepository.save(module);
        auditService.log("DEACTIVATE", "PrivilegeModule", module.getId(), "Privilege module deactivated: " + module.getCode());
        return mapper.toDTO(module);
    }

    @Override
    public PrivilegeModuleDTO reactivate(Long id) {
        PrivilegeModule module = getOrThrow(id);
        module.setActive(true);
        privilegeModuleRepository.save(module);
        auditService.log("REACTIVATE", "PrivilegeModule", module.getId(), "Privilege module reactivated: " + module.getCode());
        return mapper.toDTO(module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrivilegeModuleDTO> getAll(boolean activeOnly) {
        List<PrivilegeModule> modules = activeOnly
                ? privilegeModuleRepository.findByActiveTrueOrderByNameAsc()
                : privilegeModuleRepository.findAll();
        return modules.stream().map(mapper::toDTO).toList();
    }

    private PrivilegeModule getOrThrow(Long id) {
        return privilegeModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Privilege module not found: " + id));
    }
}
