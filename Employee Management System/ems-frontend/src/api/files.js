import api from './client';

// entityType constant used for the employee qualification certificate PDF.
// Kept in one place so the value can't drift between upload/list calls.
export const QUALIFICATION_CERT_ENTITY_TYPE = 'EMPLOYEE_QUALIFICATION_CERTIFICATE';

// Uploads one file for a given entity (e.g. an employee's certificate).
// Must be called AFTER the employee row exists, since it needs a real entityId.
export function uploadFile(file, entityType, entityId) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('entityType', entityType);
  formData.append('entityId', entityId);
  return api.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

// Lists whatever's been uploaded for one entity - used to show "current file"
// (e.g. "certificate.pdf already uploaded") when editing an employee.
export function getFiles(entityType, entityId) {
  return api.get('/api/files', { params: { entityType, entityId } });
}

// Not run through the axios client - this is a plain link the browser opens
// directly (e.g. target="_blank"), not a JSON call.
export function fileDownloadUrl(fileId) {
  return `${import.meta.env.VITE_API_BASE_URL}/api/files/${fileId}/download`;
}
