import client from "./client";

export function uploadImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return client
    .post("/admin/upload", formData, { headers: { "Content-Type": "multipart/form-data" } })
    .then((r) => r.data.url);
}
