import client from "./client";

const ORG_SLUG = import.meta.env.VITE_ORG_SLUG || "one-health";

export const registerPatient = (payload) =>
  client.post("/auth/register/patient", { ...payload, organizationSlug: ORG_SLUG }).then((r) => r.data);

export const login = (payload) =>
  client.post("/auth/login", payload).then((r) => r.data);
