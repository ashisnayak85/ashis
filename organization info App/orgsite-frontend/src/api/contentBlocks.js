import client from "./client";

export function listContentBlocks() {
  return client.get("/admin/content-blocks").then((r) => r.data);
}

export function createContentBlock(dto) {
  return client.post("/admin/content-blocks", dto).then((r) => r.data);
}

export function updateContentBlock(id, dto) {
  return client.put(`/admin/content-blocks/${id}`, dto).then((r) => r.data);
}

export function deleteContentBlock(id) {
  return client.delete(`/admin/content-blocks/${id}`);
}
