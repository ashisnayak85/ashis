import client from "./client";

export const createClinic = (payload) => client.post("/clinic-admin/clinics", payload).then((r) => r.data);

export const getMyClinics = () => client.get("/clinic-admin/clinics").then((r) => r.data);

export const getClinicDoctors = (clinicId) =>
  client.get(`/clinic-admin/clinics/${clinicId}/doctors`).then((r) => r.data);

export const getAllAssociations = () => client.get("/clinic-admin/associations").then((r) => r.data);

export const inviteDoctor = (clinicId, doctorEmail) =>
  client.post(`/clinic-admin/clinics/${clinicId}/doctors/invite`, { doctorEmail }).then((r) => r.data);

export const approveJoinRequest = (associationId) =>
  client.put(`/clinic-admin/associations/${associationId}/approve`).then((r) => r.data);

export const rejectJoinRequest = (associationId) =>
  client.put(`/clinic-admin/associations/${associationId}/reject`).then((r) => r.data);

export const removeDoctorFromClinic = (associationId) =>
  client.delete(`/clinic-admin/associations/${associationId}`).then((r) => r.data);
