import api from './client';

// Admin-only "Employee Access" - give/remove a workflow role on an
// employee, optionally scoped to one department. scopeDepartmentId = null
// means global/company-wide. See AdminEmployeeWorkflowRoleController.

export function assignEmployeeRole(employeeId, workflowRoleId, scopeDepartmentId) {
  return api.post('/api/admin/employee-roles', { employeeId, workflowRoleId, scopeDepartmentId });
}

export function revokeEmployeeRole(assignmentId) {
  return api.patch(`/api/admin/employee-roles/${assignmentId}/revoke`);
}

export function getRolesForEmployee(employeeId) {
  return api.get(`/api/admin/employee-roles/by-employee/${employeeId}`);
}

export function getEmployeesForRole(workflowRoleId) {
  return api.get(`/api/admin/employee-roles/by-role/${workflowRoleId}`);
}
