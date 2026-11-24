import api from './client';

export function getEmployeeTypes() {
  return api.get('/api/employeeTypes');
}
export function getActiveEmployeeType() {
  return api.get('/api/employeeTypes/active');
}
export function getEmployeeType(id) {
  return api.get(`/api/employeeTypes/${id}`);
}

export function createEmployeeType(dto) {
  return api.post('/api/employeeTypes', dto);
}

export function updateEmployeeType(id, dto) {
  return api.put(`/api/employeeTypes/${id}`, dto);
}

export function deleteEmployeeType(id) {
  return api.delete(`/api/employeeTypes/${id}`);
}
