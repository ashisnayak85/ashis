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

export const getAvailabilityHistory = () => client.get("/clinic-admin/availability-history").then((r) => r.data);

export const getDashboard = ({ from, to } = {}) =>
  client.get("/clinic-admin/dashboard", { params: { from, to } }).then((r) => r.data);

// --- Medicine stock (view only - selling price only, no cost/margin; owner sets pricing) ---
export const getMedicineCatalog = () => client.get("/clinic-admin/medicines").then((r) => r.data);
export const getMyStock = () => client.get("/clinic-admin/stock").then((r) => r.data);
export const updateReorderThreshold = (medicineId, reorderThreshold) =>
  client.put(`/clinic-admin/stock/${medicineId}/reorder-threshold`, { reorderThreshold }).then((r) => r.data);

// --- Billing: doctor prescribes, clinic admin bills here (see api/doctor.js for prescribing) ---
export const getPendingPrescriptions = () => client.get("/clinic-admin/prescriptions/pending").then((r) => r.data);
export const getPrescriptionDetail = (id) => client.get(`/clinic-admin/prescriptions/${id}`).then((r) => r.data);
export const billPrescription = (id, payload) =>
  client.post(`/clinic-admin/prescriptions/${id}/bill`, payload).then((r) => r.data);
export const getBillReceipt = (id) => client.get(`/clinic-admin/prescriptions/${id}/bill`).then((r) => r.data);
