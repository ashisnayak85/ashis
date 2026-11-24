import api from './client';

// Admin-only "Approval Structure Master" - now module-scoped. Renamed from
// resignationStages.js: every call takes/returns a privilegeModuleId so the
// SAME endpoints cover Resignation today and any future module. See
// AdminWorkflowStageController.

export function getWorkflowStagesForModule(privilegeModuleId) {
  return api.get(`/api/admin/workflow-stages/by-module/${privilegeModuleId}`);
}

export function createWorkflowStage(stage) {
  return api.post('/api/admin/workflow-stages', stage);
}

export function updateWorkflowStage(id, stage) {
  return api.put(`/api/admin/workflow-stages/${id}`, stage);
}

export function deactivateWorkflowStage(id) {
  return api.patch(`/api/admin/workflow-stages/${id}/deactivate`);
}

export function reactivateWorkflowStage(id) {
  return api.patch(`/api/admin/workflow-stages/${id}/reactivate`);
}

export function reorderWorkflowStages(privilegeModuleId, orderedStageIds) {
  return api.patch('/api/admin/workflow-stages/reorder', { privilegeModuleId, orderedStageIds });
}
