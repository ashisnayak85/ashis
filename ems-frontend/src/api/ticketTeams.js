import api from './client';

export function getTeam(departmentId) {
  return api.get('/api/ticket-teams', { params: { departmentId } });
}

export function addTeamMember(dto) {
  return api.post('/api/ticket-teams', dto);
}

export function removeTeamMember(id) {
  return api.delete(`/api/ticket-teams/${id}`);
}
