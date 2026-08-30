import axios from 'axios';

// Reads the XSRF-TOKEN cookie that Spring Security's CookieCsrfTokenRepository sets.
function getCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // Required so the session (JSESSIONID) and CSRF cookies are sent with every request.
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const method = (config.method || 'get').toLowerCase();
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    const token = getCookie('XSRF-TOKEN');
    if (token) {
      config.headers['X-XSRF-TOKEN'] = token;
    }
  }
  return config;
});

// Unwrap ApiResponse.data on success, surface ApiResponse.message on error.
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const raw = error.response?.data;
    // Requests made with responseType: 'blob' (file downloads) still come back
    // as a Blob even when the server actually sent a JSON error body - sniff
    // the Blob's declared type and re-parse it as JSON so error messages
    // still surface correctly instead of showing "[object Blob]".
    if (raw instanceof Blob && raw.type && raw.type.includes('json')) {
      return raw.text().then((text) => {
        let data = {};
        try { data = JSON.parse(text); } catch { /* leave data empty */ }
        return Promise.reject(buildRejection(error, data));
      });
    }
    return Promise.reject(buildRejection(error, raw));
  }
);

function buildRejection(error, data) {
  const errors = data?.errors;
  const message = data?.message || error.message || 'Something went wrong';
  return { ...error, message, errors, status: error.response?.status };
}

export default api;
