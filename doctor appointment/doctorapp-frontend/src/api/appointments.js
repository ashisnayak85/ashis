import client from "./client";

export const bookAppointment = (slotId) =>
  client.post("/patient/appointments", { slotId }).then((r) => r.data);

export const getMyAppointments = () =>
  client.get("/patient/appointments").then((r) => r.data);

export const cancelAppointment = (id) =>
  client.put(`/patient/appointments/${id}/cancel`).then((r) => r.data);
