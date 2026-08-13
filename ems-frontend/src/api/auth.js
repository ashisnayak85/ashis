import api from './client';

// Spring's formLogin expects application/x-www-form-urlencoded username/password,
// not JSON - so we build a URLSearchParams body here instead of a plain object.
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
