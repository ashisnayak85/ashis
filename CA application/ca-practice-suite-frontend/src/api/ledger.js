import api from './client';
import { downloadBlob } from './download';

export function searchLedger(params) {
  return api.get('/api/ledger/search', { params });
}
export function postEntry(dto) {
  return api.post('/api/ledger', dto);
}
export function toggleReconciled(id) {
  return api.put(`/api/ledger/${id}/reconcile`);
}
export function deleteEntry(id) {
  return api.delete(`/api/ledger/${id}`);
}
export async function exportLedger(params) {
  const blob = await api.get('/api/ledger/export', { params, responseType: 'blob' });
  downloadBlob(blob, `ledger-${new Date().toISOString().slice(0, 10)}.xlsx`);
}
