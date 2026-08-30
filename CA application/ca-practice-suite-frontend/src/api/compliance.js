import api from './client';

export function searchCompliance(params) {
  return api.get('/api/compliance', { params });
}
export function getUpcoming(limit = 10) {
  return api.get('/api/compliance/upcoming', { params: { limit } });
}
export function createTask(dto) {
  return api.post('/api/compliance', dto);
}
export function updateTask(id, dto) {
  return api.put(`/api/compliance/${id}`, dto);
}
export function markFiled(id, remarks) {
  return api.put(`/api/compliance/${id}/file`, null, { params: { remarks } });
}
export function deleteTask(id) {
  return api.delete(`/api/compliance/${id}`);
}
