import client from "./client";

export const getClinics = () => client.get("/patient/clinics").then((r) => r.data);

export const getDoctorsAtClinic = (clinicId) =>
  client.get(`/patient/clinics/${clinicId}/doctors`).then((r) => r.data);

export const getSlots = (doctorId, clinicId, date) =>
  client.get("/patient/slots", { params: { doctorId, clinicId, date } }).then((r) => r.data);

export const bookAppointment = (slotId) =>
  client.post("/patient/appointments", { slotId }).then((r) => r.data);

export const cancelAppointment = (id) =>
  client.put(`/patient/appointments/${id}/cancel`).then((r) => r.data);

export const getMyAppointments = () => client.get("/patient/appointments").then((r) => r.data);
