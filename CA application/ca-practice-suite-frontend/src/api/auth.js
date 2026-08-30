import api from './client';

// Spring's formLogin expects application/x-www-form-urlencoded username/password.
export function login(username, password) {
  const body = new URLSearchParams();
  body.append('username', username);
  body.append('password', password);
  return api.post('/login', body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
}

export function logout() {
  return api.post('/logout');
}

export function me() {
  return api.get('/api/auth/me');
}

export function changePassword(currentPassword, newPassword) {
  return api.post('/api/auth/change-password', { currentPassword, newPassword });
}
