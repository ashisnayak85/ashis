import api from './client';

export function searchClients(params) {
  return api.get('/api/clients', { params });
}
export function getActiveClients() {
  return api.get('/api/clients/active');
}
export function getClient(id) {
  return api.get(`/api/clients/${id}`);
}
export function createClient(dto) {
  return api.post('/api/clients', dto);
}
export function updateClient(id, dto) {
  return api.put(`/api/clients/${id}`, dto);
}
export function deactivateClient(id) {
  return api.delete(`/api/clients/${id}`);
}
