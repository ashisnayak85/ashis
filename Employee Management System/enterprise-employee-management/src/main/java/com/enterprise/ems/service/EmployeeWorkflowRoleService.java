package com.enterprise.ems.service;

import com.enterprise.ems.dto.EmployeeWorkflowRoleDTO;

import java.util.List;

/*
 * PURPOSE: Admin-facing "Employee Access" - assign a WorkflowRole to an
 * employee (optionally scoped to a department), or revoke it. This is what
 * ResignationServiceImpl.resolveApprover() reads from at submit time - see
 * EmployeeWorkflowRole entity javadoc. No programmer involvement needed to
 * change who holds an approval role.
 */
public interface EmployeeWorkflowRoleService {

    EmployeeWorkflowRoleDTO assign(Long employeeId, Long workflowRoleId, Long scopeDepartmentId, Long assignedByEmployeeId);

    EmployeeWorkflowRoleDTO revoke(Long assignmentId, Long removedByEmployeeId);

    List<EmployeeWorkflowRoleDTO> getForEmployee(Long employeeId);

    List<EmployeeWorkflowRoleDTO> getForRole(Long workflowRoleId);
}
