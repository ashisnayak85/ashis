import api from './client';

// Admin/Manager marking or correcting attendance for any employee, any date.
export function markAttendance(dto) {
  return api.post('/api/attendance', dto);
}

// Employee marking their OWN attendance for today. dto only needs
// { status, checkInTime, checkOutTime, remarks } - employeeId/date are
// resolved and forced server-side.
export function markMyAttendance(dto) {
  return api.post('/api/attendance/self', dto);
}

export function getAttendanceByEmployee(employeeId, { page = 0, size = 10 } = {}) {
  return api.get(`/api/attendance/employee/${employeeId}`, { params: { page, size } });
}

// Combined filter: employeeId, startDate, endDate, and status are all
// optional - pass only what the user actually picked and the backend
// applies just those filters. For a plain employee login the backend ignores
// employeeId and always scopes to their own records, so this same call also
// works as "my attendance history".
export function searchAttendance({ employeeId, startDate, endDate, status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (employeeId) params.employeeId = employeeId;
  if (startDate) params.startDate = startDate;
  if (endDate) params.endDate = endDate;
  if (status) params.status = status;
  return api.get('/api/attendance/search', { params });
}
