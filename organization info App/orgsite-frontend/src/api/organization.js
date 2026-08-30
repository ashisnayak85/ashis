import client from "./client";

export function getPublicOrg(slug) {
  return client.get(`/public/org/${slug}`).then((r) => r.data);
}

export function getMyOrganization() {
  return client.get("/admin/organization").then((r) => r.data);
}

export function updateMyOrganization(dto) {
  return client.put("/admin/organization", dto).then((r) => r.data);
}

export function setPublished(published) {
  return client.patch(`/admin/organization/publish?published=${published}`).then((r) => r.data);
}
