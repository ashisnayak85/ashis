package com.enterprise.ems.service;

import com.enterprise.ems.dto.WorkflowRoleDTO;

import java.util.List;

/*
 * PURPOSE: Admin-facing CRUD for the WorkflowRole master - "Role creation"
 * from the admin's point of view. No programmer involvement needed to add,
 * rename, or retire a business role used in approval routing.
 */
public interface WorkflowRoleService {

    WorkflowRoleDTO create(String name, String description);

    WorkflowRoleDTO update(Long id, String name, String description);

    WorkflowRoleDTO deactivate(Long id);

    WorkflowRoleDTO reactivate(Long id);

    WorkflowRoleDTO getById(Long id);

    List<WorkflowRoleDTO> getAll(boolean activeOnly);
}
