import api from './client';
import { downloadBlob, todayStamp } from './download';

export function applyLeave(dto) {
  return api.post('/api/leaves', dto);
}

export function approveLeave(id) {
  return api.put(`/api/leaves/${id}/approve`);
}

export function rejectLeave(id) {
  return api.put(`/api/leaves/${id}/reject`);
}

export function getPendingLeaves() {
  return api.get('/api/leaves/pending');
}

// Admin/manager: search leave requests across all employees. All filters optional.
// from/to are ISO date strings (yyyy-MM-dd) and are an overlap range - a leave
// that spans into the range is included even if it started before "from".
export function searchLeaves({ status, from, to, employeeName, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  if (employeeName) params.employeeName = employeeName;
  return api.get('/api/leaves/search', { params });
}

// Self-service: "my leave history". status and date range (from/to, overlap
// semantics - see searchLeaves) are both optional.
export function getMyLeaves({ status, from, to, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  return api.get('/api/leaves/my', { params });
}

// Admin/manager: export the currently filtered leave-request list as .xlsx.
// Same filters as searchLeaves, minus pagination - the export always contains
// the full filtered result set, not just the current page.
export async function exportLeaves({ status, from, to, employeeName } = {}) {
  const params = {};
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  if (employeeName) params.employeeName = employeeName;
  const blob = await api.get('/api/leaves/export', { params, responseType: 'blob' });
  downloadBlob(blob, `leave-requests-${todayStamp()}.xlsx`);
}

// Self-service: export the logged-in employee's own leave history as .xlsx.
// Same filters as getMyLeaves, minus pagination.
export async function exportMyLeaves({ status, from, to } = {}) {
  const params = {};
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  const blob = await api.get('/api/leaves/my/export', { params, responseType: 'blob' });
  downloadBlob(blob, `my-leave-history-${todayStamp()}.xlsx`);
}
