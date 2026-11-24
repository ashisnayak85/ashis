import api from './client';

export function getUsers() {
  return api.get('/api/admin/users');
}

export function createUser(dto) {
  return api.post('/api/admin/users', dto);
}

export function getRoles() {
  return api.get('/api/admin/users/roles');
}

export function updateUserRole(id, roleName) {
  return api.put(`/api/admin/users/${id}/role`, { roleName });
}
