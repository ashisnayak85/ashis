import client from "./client";

export function register({ email, password, organizationName, category }) {
  return client.post("/auth/register", { email, password, organizationName, category }).then((r) => r.data);
}

export function login({ email, password }) {
  return client.post("/auth/login", { email, password }).then((r) => r.data);
}

export function saveSession(auth) {
  localStorage.setItem("accessToken", auth.accessToken);
  localStorage.setItem("refreshToken", auth.refreshToken);
  localStorage.setItem("email", auth.email);
  localStorage.setItem("organizationSlug", auth.organizationSlug || "");
}

export function clearSession() {
  localStorage.clear();
}

export function isLoggedIn() {
  return !!localStorage.getItem("accessToken");
}

export function getOrganizationSlug() {
  return localStorage.getItem("organizationSlug") || "";
}
