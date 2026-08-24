import client from "./client";

export const registerPatient = (payload) =>
  client.post("/auth/register/patient", payload).then((r) => r.data);

export const registerDoctor = (payload) =>
  client.post("/auth/register/doctor", payload).then((r) => r.data);

export const registerClinicAdmin = (payload) =>
  client.post("/auth/register/clinic-admin", payload).then((r) => r.data);

export const login = (payload) =>
  client.post("/auth/login", payload).then((r) => r.data);
