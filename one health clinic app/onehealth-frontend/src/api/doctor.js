import client from "./client";

export const getMyClinics = () => client.get("/doctor/my-clinics").then((r) => r.data);

export const getMyAvailability = () => client.get("/doctor/availability").then((r) => r.data);

export const addAvailability = (payload) => client.post("/doctor/availability", payload).then((r) => r.data);

export const deleteAvailability = (id) => client.delete(`/doctor/availability/${id}`).then((r) => r.data);

export const getMyAppointments = (date) =>
  client.get("/doctor/appointments", { params: { date } }).then((r) => r.data);

export const updateAppointmentStatus = (id, status) =>
  client.put(`/doctor/appointments/${id}/status`, { status }).then((r) => r.data);

// --- Prescribing (writes off this appointment's clinic's medicine stock) ---
export const getAvailableMedicines = (appointmentId) =>
  client.get(`/doctor/appointments/${appointmentId}/available-medicines`).then((r) => r.data);

export const createPrescription = (appointmentId, payload) =>
  client.post(`/doctor/appointments/${appointmentId}/prescriptions`, payload).then((r) => r.data);

export const getPrescription = (appointmentId) =>
  client.get(`/doctor/appointments/${appointmentId}/prescriptions`).then((r) => r.data);
