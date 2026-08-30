import api from './client';

export function getUsers() {
  return api.get('/api/admin/users');
}
export function getRoles() {
  return api.get('/api/admin/users/roles');
}
export function createUser(dto) {
  return api.post('/api/admin/users', dto);
}
export function setUserEnabled(id, enabled) {
  return api.put(`/api/admin/users/${id}/enabled`, null, { params: { enabled } });
}
