package com.enterprise.ems.service;

import com.enterprise.ems.dto.PrivilegeModuleDTO;

import java.util.List;

/*
 * PURPOSE: Admin-facing view/toggle of the module master that powers the
 * module dropdown on the unified Workflow Setup screen. Deliberately has NO
 * create() - see PrivilegeModuleInitializer / SystemModule for why: a
 * module's `code` is a hardcoded contract with the Java service that
 * filters WorkflowStageRepository by it (e.g.
 * AppConstants.MODULE_CODE_RESIGNATION in ResignationServiceImpl). Letting
 * admin type an arbitrary code here would let them build a whole approval
 * chain under a code no service is ever listening for - a silent,
 * hard-to-diagnose mismatch. New modules are registered automatically by
 * the app itself on startup instead - see PrivilegeModuleInitializer.
 */
public interface PrivilegeModuleService {

    PrivilegeModuleDTO deactivate(Long id);

    PrivilegeModuleDTO reactivate(Long id);

    List<PrivilegeModuleDTO> getAll(boolean activeOnly);
}
