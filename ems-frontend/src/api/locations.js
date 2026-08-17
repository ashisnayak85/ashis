import api from './client';

export function getLocations() {
  return api.get('/api/locations');
}

// Lightweight list for dropdowns/pickers
export function getActiveLocations() {
  return api.get('/api/locations/active');
}

export function getLocation(id) {
  return api.get(`/api/locations/${id}`);
}

export function createLocation(dto) {
  return api.post('/api/locations', dto);
}

export function updateLocation(id, dto) {
  return api.put(`/api/locations/${id}`, dto);
}

export function deleteLocation(id) {
  return api.delete(`/api/locations/${id}`);
}
