import api from './client';

// Admin/Manager marking or correcting attendance for any employee, any date.
export function markAttendance(dto) {
  return api.post('/api/attendance', dto);
}

// Employee self-service: punch in / punch out for today, server clock only.
// remarks is optional. imageBlob is the live webcam capture - only actually
// required/checked server-side when face verification is switched on (see
// getFaceStatus below); when the feature is off, pass null and the backend
// ignores the absence entirely. Always sent as multipart/form-data so the
// same endpoint works either way, with or without a photo attached.
function buildPunchForm(remarks, imageBlob) {
  const form = new FormData();
  if (remarks) form.append('remarks', remarks);
  if (imageBlob) form.append('image', imageBlob, 'punch.jpg');
  return form;
}

export function punchIn(remarks, imageBlob) {
  return api.post('/api/attendance/self/punch-in', buildPunchForm(remarks, imageBlob));
}

export function punchOut(remarks, imageBlob) {
  return api.post('/api/attendance/self/punch-out', buildPunchForm(remarks, imageBlob));
}

// Whether face verification is switched on system-wide (enabled), and
// whether the logged-in employee has already completed their one-time face
// enrollment (enrolled). The attendance page drives all camera-step UI off
// this single call - flipping attendance.face-verification.enabled in the
// backend's application.properties is enough to change what this returns,
// no frontend code changes needed.
export function getFaceStatus() {
  return api.get('/api/attendance/self/face/status');
}

// One-time (or re-doable) capture of the employee's own reference face.
export function enrollFace(imageBlob) {
  const form = new FormData();
  form.append('image', imageBlob, 'enrollment.jpg');
  return api.post('/api/attendance/self/face/enroll', form);
}

// --- Admin/Manager face management (any employee, not just self) ---

export function getAdminFaceStatus(employeeId) {
  return api.get(`/api/attendance/admin/face/status/${employeeId}`);
}

// Admin (re-)enrolling a given employee's face. If they already had one
// enrolled, the previous embedding + photo are backed up server-side first.
export function adminEnrollFace(employeeId, imageBlob) {
  const form = new FormData();
  form.append('image', imageBlob, 'enrollment.jpg');
  return api.post(`/api/attendance/admin/face/enroll/${employeeId}`, form);
}

// Diagnostic tool: runs the real comparison but returns the actual
// similarity score + threshold instead of just pass/fail, so an admin can
// see WHY a punch-in is being rejected.
export function adminTestVerifyFace(employeeId, imageBlob) {
  const form = new FormData();
  form.append('image', imageBlob, 'test.jpg');
  return api.post(`/api/attendance/admin/face/test-verify/${employeeId}`, form);
}

export function getFaceHistory(employeeId) {
  return api.get(`/api/attendance/admin/face/history/${employeeId}`);
}

// Today's attendance row for the logged-in employee, or null if they haven't
// punched in yet - used to decide whether to show "Punch In" or "Punch Out".
export function getMyTodayStatus() {
  return api.get('/api/attendance/self/today');
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
