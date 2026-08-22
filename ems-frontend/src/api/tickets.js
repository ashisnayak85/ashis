import api from './client';

export function createTicket(dto) {
  return api.post('/api/tickets', dto);
}

export function getTicket(id) {
  return api.get(`/api/tickets/${id}`);
}

export function getConversation(id) {
  return api.get(`/api/tickets/${id}/conversation`);
}

// Tickets I raised.
export function getMyTickets({ status, priority, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (priority) params.priority = priority;
  return api.get('/api/tickets/my', { params });
}

// Tickets currently assigned to me.
export function getAssignedTickets({ status, priority, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (priority) params.priority = priority;
  return api.get('/api/tickets/assigned', { params });
}

// Unclaimed pool for departments I'm on the ticket team for (OPEN + unassigned).
export function getClaimableTickets({ priority, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (priority) params.priority = priority;
  return api.get('/api/tickets/claimable', { params });
}

// Admin/manager: every ticket, any department.
export function searchTickets({ departmentId, status, priority, keyword, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (departmentId) params.departmentId = departmentId;
  if (status) params.status = status;
  if (priority) params.priority = priority;
  if (keyword) params.keyword = keyword;
  return api.get('/api/tickets/search', { params });
}

export function markRead(id) {
  return api.post(`/api/tickets/${id}/read`);
}

export function reply(id, message, parentEntryId) {
  return api.post(`/api/tickets/${id}/reply`, { message, parentEntryId });
}

// --- Responsible-person actions ---
export function claimTicket(id) {
  return api.post(`/api/tickets/${id}/claim`);
}
export function resolveTicket(id, message) {
  return api.post(`/api/tickets/${id}/resolve`, { message });
}
export function rejectTicket(id, message) {
  return api.post(`/api/tickets/${id}/reject`, { message });
}
export function transferTicket(id, targetEmployeeId, message) {
  return api.post(`/api/tickets/${id}/transfer`, { targetEmployeeId, message });
}

// --- Raiser (user-side) actions ---
export function acceptResolution(id) {
  return api.post(`/api/tickets/${id}/accept`);
}
export function escalateTicket(id, message) {
  return api.post(`/api/tickets/${id}/escalate`, { message });
}
export function closeTicket(id, message) {
  return api.post(`/api/tickets/${id}/close`, { message });
}
