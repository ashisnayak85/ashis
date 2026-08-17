import api from './client';

export function getDepartments() {
  return api.get('/api/departments');
}
export function getActiveDepartments() {
  return api.get('/api/departments/active');
}
export function getDepartment(id) {
  return api.get(`/api/departments/${id}`);
}

export function createDepartment(dto) {
  return api.post('/api/departments', dto);
}

export function updateDepartment(id, dto) {
  return api.put(`/api/departments/${id}`, dto);
}

export function deleteDepartment(id) {
  return api.delete(`/api/departments/${id}`);
}
