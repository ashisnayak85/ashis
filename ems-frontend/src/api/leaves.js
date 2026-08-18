import api from './client';

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

// Self-service: "my leave history". status is optional (PENDING/APPROVED/REJECTED).
export function getMyLeaves({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  return api.get('/api/leaves/my', { params });
}
