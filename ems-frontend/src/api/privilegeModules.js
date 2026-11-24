import api from './client';

// Admin-only module master - backs the module dropdown on the unified
// Workflow Setup screen. Deliberately has NO create function - a module's
// code is a hardcoded contract with the backend service that filters by it
// (see AppConstants.MODULE_CODE_* / SystemModule on the backend). New
// modules are registered automatically by the app on startup, never
// created through this API. See AdminPrivilegeModuleController.

export function getPrivilegeModules(activeOnly = false) {
  return api.get('/api/admin/privilege-modules', { params: { activeOnly } });
}

export function deactivatePrivilegeModule(id) {
  return api.patch(`/api/admin/privilege-modules/${id}/deactivate`);
}

export function reactivatePrivilegeModule(id) {
  return api.patch(`/api/admin/privilege-modules/${id}/reactivate`);
}
