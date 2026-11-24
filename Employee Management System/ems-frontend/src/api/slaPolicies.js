import api from './client';

export function getSlaPolicies(departmentId) {
  return api.get('/api/sla-policies', { params: { departmentId } });
}

export function saveSlaPolicy(dto) {
  return api.post('/api/sla-policies', dto);
}
