import client from "./client";

export const getMyClinicDoctors = () => client.get("/clinic-admin/doctors").then((r) => r.data);

export const getSlots = (doctorId, date) =>
  client.get("/clinic-admin/slots", { params: { doctorId, date } }).then((r) => r.data);

export const bookWalkIn = (payload) =>
  client.post("/clinic-admin/appointments/walk-in", payload).then((r) => r.data);

export const getAppointments = (date) =>
  client.get("/clinic-admin/appointments", { params: { date } }).then((r) => r.data);

export const updateAppointmentStatus = (id, status) =>
  client.put(`/clinic-admin/appointments/${id}/status`, { status }).then((r) => r.data);

export const cancelAppointment = (id) =>
  client.put(`/clinic-admin/appointments/${id}/cancel`).then((r) => r.data);

export const getClinicAvailability = () => client.get("/clinic-admin/availability").then((r) => r.data);

export const setAvailabilityActive = (id, active) =>
  client.put(`/clinic-admin/availability/${id}/status`, null, { params: { active } }).then((r) => r.data);

export const getDashboard = ({ from, to } = {}) =>
  client.get("/clinic-admin/dashboard", { params: { from, to } }).then((r) => r.data);
