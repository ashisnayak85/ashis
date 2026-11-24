import api from './client';

// Admin-only "Role Master" - business roles used for approval routing
// (HOD, HR Head, ...), separate from login/auth roles. See
// AdminWorkflowRoleController on the backend.

export function getWorkflowRoles(activeOnly = false) {
  return api.get('/api/admin/workflow-roles', { params: { activeOnly } });
}

export function getWorkflowRole(id) {
  return api.get(`/api/admin/workflow-roles/${id}`);
}

export function createWorkflowRole(name, description) {
  return api.post('/api/admin/workflow-roles', { name, description });
}

export function updateWorkflowRole(id, name, description) {
  return api.put(`/api/admin/workflow-roles/${id}`, { name, description });
}

export function deactivateWorkflowRole(id) {
  return api.patch(`/api/admin/workflow-roles/${id}/deactivate`);
}

export function reactivateWorkflowRole(id) {
  return api.patch(`/api/admin/workflow-roles/${id}/reactivate`);
}
