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
    const data = error.response?.data;
    // GlobalExceptionHandler puts per-field messages here on @Valid failures,
    // e.g. ["Location name is required", "Enter a valid contact number..."].
    // Previously this was dropped, so callers only ever saw the generic
    // "Validation failed" text in data.message.
    const errors = data?.errors;
    const message = data?.message || error.message || 'Something went wrong';
    return Promise.reject({ ...error, message, errors, status: error.response?.status });
  }
);

export default api;
