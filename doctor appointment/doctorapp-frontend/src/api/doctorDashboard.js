import client from "./client";

export const getMyDoctorProfile = () => client.get("/doctor/me/profile").then((r) => r.data);

export const getMyDoctorAppointments = () => client.get("/doctor/appointments").then((r) => r.data);

export const updateAppointmentStatus = (id, status) =>
  client.put(`/doctor/appointments/${id}/status`, { status }).then((r) => r.data);

export const getMyPatients = () => client.get("/doctor/patients").then((r) => r.data);

export const addAvailability = (payload) => client.post("/doctor/availability", payload).then((r) => r.data);

export const getMyAvailability = () => client.get("/doctor/availability").then((r) => r.data);

export const deleteAvailability = (availabilityId) =>
  client.delete(`/doctor/availability/${availabilityId}`).then((r) => r.data);

// --- Clinic associations (replaces the old doctor-owned "addClinic") ---

export const browseClinics = (city) =>
  client.get("/doctor/clinics/browse", { params: city ? { city } : {} }).then((r) => r.data);

export const getMyClinicAssociations = () => client.get("/doctor/clinics").then((r) => r.data);

export const requestJoinClinic = (clinicId) =>
  client.post(`/doctor/clinics/${clinicId}/join-request`).then((r) => r.data);

export const approveClinicInvite = (associationId) =>
  client.put(`/doctor/clinics/associations/${associationId}/approve`).then((r) => r.data);

export const rejectClinicInvite = (associationId) =>
  client.put(`/doctor/clinics/associations/${associationId}/reject`).then((r) => r.data);

export const leaveClinic = (associationId) =>
  client.delete(`/doctor/clinics/associations/${associationId}`).then((r) => r.data);
