import api from './client';
import { downloadBlob, todayStamp } from './download';

// Active employees only (mirrors old /employees Thymeleaf page)
export function getEmployees({ page = 0, size = 10, search = '' } = {}) {
  return api.get('/api/employees', { params: { page, size, search: search || undefined } });
}

// Every row in the Employee table, active or not (mirrors allEmployeeList.html)
export function getAllEmployees({ page = 0, size = 10, search = '' } = {}) {
  return api.get('/api/allEmployees', { params: { page, size, search: search || undefined } });
}

// Same "search" filter as getEmployees, minus pagination - exports every
// matching active employee as .xlsx.
export async function exportEmployees({ search = '' } = {}) {
  const params = { search: search || undefined };
  const blob = await api.get('/api/employees/export', { params, responseType: 'blob' });
  downloadBlob(blob, `employees-${todayStamp()}.xlsx`);
}

// Same "search" filter as getAllEmployees, minus pagination - exports every
// matching row (active or not) as .xlsx.
export async function exportAllEmployees({ search = '' } = {}) {
  const params = { search: search || undefined };
  const blob = await api.get('/api/allEmployees/export', { params, responseType: 'blob' });
  downloadBlob(blob, `all-employees-${todayStamp()}.xlsx`);
}

// Active employees who don't have a login yet - for the "New User" picker
export function getAvailableForUser() {
  return api.get('/api/employees/available-for-user');
}

export function getEmployee(id) {
  return api.get(`/api/employees/${id}`);
}

export function createEmployee(dto) {
  return api.post('/api/employees', dto);
}

export function updateEmployee(id, dto) {
  return api.put(`/api/employees/${id}`, dto);
}

export function deleteEmployee(id) {
  return api.delete(`/api/employees/${id}`);
}
