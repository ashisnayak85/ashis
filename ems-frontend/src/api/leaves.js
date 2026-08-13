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
