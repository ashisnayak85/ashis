import api from './client';

export function getDesignations() {
  return api.get('/api/designations');
}
export function getActiveDesignations() {
  return api.get('/api/designations/active');
}
export function getDesignation(id) {
  return api.get(`/api/designations/${id}`);
}

export function createDesignation(dto) {
  return api.post('/api/designations', dto);
}

export function updateDesignation(id, dto) {
  return api.put(`/api/designations/${id}`, dto);
}

export function deleteDesignation(id) {
  return api.delete(`/api/designations/${id}`);
}
