import api from './client';

export function markAttendance(dto) {
  return api.post('/api/attendance', dto);
}

export function getAttendanceByEmployee(employeeId, { page = 0, size = 10 } = {}) {
  return api.get(`/api/attendance/employee/${employeeId}`, { params: { page, size } });
}
