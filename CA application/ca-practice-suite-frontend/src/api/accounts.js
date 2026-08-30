import api from './client';

export function getAccounts() {
  return api.get('/api/accounts');
}
export function createAccount(dto) {
  return api.post('/api/accounts', dto);
}
export function updateAccount(id, dto) {
  return api.put(`/api/accounts/${id}`, dto);
}
export function deleteAccount(id) {
  return api.delete(`/api/accounts/${id}`);
}
