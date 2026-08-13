import api from './client';

export function getUsers() {
  return api.get('/api/admin/users');
}

export function createUser(dto) {
  return api.post('/api/admin/users', dto);
}
