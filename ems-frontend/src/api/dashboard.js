import api from './client';

export function getStats() {
  return api.get('/api/dashboard/stats');
}

// Personal dashboard for a plain employee login - own attendance/leave summary only.
export function getMyStats() {
  return api.get('/api/dashboard/my-stats');
}
