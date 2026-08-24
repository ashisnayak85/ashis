import client from "./client";

export const getDashboardStats = () => client.get("/admin/dashboard").then((r) => r.data);

export const getDoctors = (verified) =>
  client
    .get("/admin/doctors", { params: verified === undefined ? {} : { verified } })
    .then((r) => r.data);

export const verifyDoctor = (id) => client.put(`/admin/doctors/${id}/verify`).then((r) => r.data);

export const setDoctorStatus = (id, active) =>
  client.put(`/admin/doctors/${id}/status`, null, { params: { active } }).then((r) => r.data);

export const getPatients = () => client.get("/admin/patients").then((r) => r.data);

export const getAllAppointments = () => client.get("/admin/appointments").then((r) => r.data);

export const getClinics = (verified) =>
  client
    .get("/admin/clinics", { params: verified === undefined ? {} : { verified } })
    .then((r) => r.data);

export const verifyClinic = (id) => client.put(`/admin/clinics/${id}/verify`).then((r) => r.data);

export const setClinicStatus = (id, active) =>
  client.put(`/admin/clinics/${id}/status`, null, { params: { active } }).then((r) => r.data);
