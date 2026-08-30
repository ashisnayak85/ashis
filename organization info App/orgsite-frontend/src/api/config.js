// The backend serves uploaded images from its root (e.g. http://localhost:8083/uploads/...),
// not under /api, so derive the origin separately from the API base URL.
const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8083/api";
export const BACKEND_ORIGIN = API_BASE.replace(/\/api\/?$/, "");

export function resolveImageUrl(url) {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return `${BACKEND_ORIGIN}${url}`;
}
