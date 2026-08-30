import api from './client';

export function searchInvoices(params) {
  return api.get('/api/invoices', { params });
}
export function getInvoice(id) {
  return api.get(`/api/invoices/${id}`);
}
export function createInvoice(dto) {
  return api.post('/api/invoices', dto);
}
export function updateInvoiceStatus(id, status) {
  return api.put(`/api/invoices/${id}/status`, null, { params: { status } });
}
export function deleteInvoice(id) {
  return api.delete(`/api/invoices/${id}`);
}
