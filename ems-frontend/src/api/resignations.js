import api from './client';

// --- Employee self-service ---

export function submitResignation(reason, noticePeriodDays) {
  return api.post('/api/resignations', { reason, noticePeriodDays });
}

export function getMyResignation() {
  return api.get('/api/resignations/my');
}

export function withdrawResignation(id) {
  return api.post(`/api/resignations/${id}/withdraw`);
}

// --- Viewing ---

export function getResignation(id) {
  return api.get(`/api/resignations/${id}`);
}

// Admin/HR: every resignation, filterable by status.
export function searchResignations({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  return api.get('/api/resignations/search', { params });
}

// --- Approver inbox / actions ---

export function getPendingApprovalsForMe() {
  return api.get('/api/resignations/pending-approvals');
}

export function approveStage(id, remarks) {
  return api.post(`/api/resignations/${id}/approve`, { remarks });
}

export function rejectStage(id, remarks) {
  return api.post(`/api/resignations/${id}/reject`, { remarks });
}

// --- No-dues clearance ---

export function clearDepartment(id, departmentId, remarks) {
  return api.post(`/api/resignations/${id}/clearance/clear`, { departmentId, remarks });
}

export function blockDepartment(id, departmentId, remarks) {
  return api.post(`/api/resignations/${id}/clearance/block`, { departmentId, remarks });
}

// --- Asset return line items ---

export function addAsset(id, assetDescription) {
  return api.post(`/api/resignations/${id}/assets`, { assetDescription });
}

export function markAssetReturned(id, assetId, remarks) {
  return api.post(`/api/resignations/${id}/assets/${assetId}/returned`, { remarks });
}

// --- Final approval / last-working-day override ---

export function finalApprove(id) {
  return api.post(`/api/resignations/${id}/final-approve`);
}

export function overrideLastWorkingDay(id, actualLastWorkingDay, overrideReason) {
  return api.post(`/api/resignations/${id}/override-lwd`, { actualLastWorkingDay, overrideReason });
}
