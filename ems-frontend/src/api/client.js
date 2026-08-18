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
    // GlobalExceptionHandler puts human-readable messages here on @Valid failures,
    // e.g. ["Email is required", "Enter a valid 10-digit Indian mobile number"].
    const errors = data?.errors;
    // On a validation failure, ApiResponse.data is a {fieldName: message} map
    // instead of the usual resource payload - lets a form highlight the exact
    // input instead of only showing a generic banner.
    const fieldErrors = data?.data && typeof data.data === 'object' && !Array.isArray(data.data)
      ? data.data
      : null;
    const message = data?.message || error.message || 'Something went wrong';
    return Promise.reject({ ...error, message, errors, fieldErrors, status: error.response?.status });
  }
);

export default api;
