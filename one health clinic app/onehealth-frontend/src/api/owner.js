import client from "./client";

// --- Clinics (branches) ---
export const getClinics = () => client.get("/owner/clinics").then((r) => r.data);
export const createClinic = (payload) => client.post("/owner/clinics", payload).then((r) => r.data);
export const setClinicStatus = (id, active) =>
  client.put(`/owner/clinics/${id}/status`, null, { params: { active } }).then((r) => r.data);
export const registerClinicAdmin = (payload) =>
  client.post("/owner/clinic-admins", payload).then((r) => r.data);

// --- Doctors ---
export const getDoctors = () => client.get("/owner/doctors").then((r) => r.data);
export const registerDoctor = (payload) => client.post("/owner/doctors", payload).then((r) => r.data);
export const setDoctorStatus = (id, active) =>
  client.put(`/owner/doctors/${id}/status`, null, { params: { active } }).then((r) => r.data);
export const assignDoctor = (doctorId, clinicId) =>
  client.post("/owner/doctors/assign", { doctorId, clinicId }).then((r) => r.data);
export const unassignDoctor = (doctorId, clinicId) =>
  client.post("/owner/doctors/unassign", { doctorId, clinicId }).then((r) => r.data);
export const updateDoctorSpecializations = (doctorId, specializationIds) =>
  client.put(`/owner/doctors/${doctorId}/specializations`, { specializationIds }).then((r) => r.data);

// --- Specializations (master list) ---
export const getSpecializations = () => client.get("/owner/specializations").then((r) => r.data);
export const createSpecialization = (name) =>
  client.post("/owner/specializations", { name }).then((r) => r.data);
export const renameSpecialization = (id, name) =>
  client.put(`/owner/specializations/${id}`, { name }).then((r) => r.data);
export const setSpecializationStatus = (id, active) =>
  client.put(`/owner/specializations/${id}/status`, null, { params: { active } }).then((r) => r.data);
export const deleteSpecialization = (id) =>
  client.delete(`/owner/specializations/${id}`).then((r) => r.data);

// --- Employees / HR ---
export const getEmployees = () => client.get("/owner/employees").then((r) => r.data);
export const getEmployeeProfile = (userId) => client.get(`/owner/employees/${userId}`).then((r) => r.data);
export const updateEmployeeProfile = (userId, payload) =>
  client.put(`/owner/employees/${userId}/profile`, payload).then((r) => r.data);
export const addSalaryRecord = (userId, payload) =>
  client.post(`/owner/employees/${userId}/salary`, payload).then((r) => r.data);

// --- Doctor time periods (availability) ---
export const getOrganizationAvailability = (clinicId) =>
  client.get("/owner/availability", { params: { clinicId } }).then((r) => r.data);
export const setAvailabilityActive = (id, active) =>
  client.put(`/owner/availability/${id}/status`, null, { params: { active } }).then((r) => r.data);
export const getAvailabilityHistory = (clinicId) =>
  client.get("/owner/availability-history", { params: { clinicId } }).then((r) => r.data);

// --- Dashboard ---
export const getDashboard = ({ from, to, clinicId } = {}) =>
  client.get("/owner/dashboard", { params: { from, to, clinicId } }).then((r) => r.data);

// --- Medicine catalog (org-wide) ---
export const getMedicines = () => client.get("/owner/medicines").then((r) => r.data);
export const createMedicine = (payload) => client.post("/owner/medicines", payload).then((r) => r.data);
export const setMedicineStatus = (id, active) =>
  client.put(`/owner/medicines/${id}/status`, null, { params: { active } }).then((r) => r.data);

// --- Stock dispatch: owner sends stock to a branch (clinic admin no longer adds stock directly) ---
export const dispatchStock = (payload) => client.post("/owner/stock/dispatch", payload).then((r) => r.data);
export const getClinicStockForOwner = (clinicId) => client.get(`/owner/stock/${clinicId}`).then((r) => r.data);
export const getStockReconciliation = () => client.get("/owner/stock/reconciliation").then((r) => r.data);

// --- Margin/pricing: OWNER-ONLY - clinic admins can no longer set their own margin ---
export const updateMargin = (clinicId, medicineId, marginPercent) =>
  client.put(`/owner/clinics/${clinicId}/stock/${medicineId}/margin`, { marginPercent }).then((r) => r.data);

// --- Prescriptions (owner-only full cost/profit view) ---
export const getPrescriptionFullDetail = (id) => client.get(`/owner/prescriptions/${id}`).then((r) => r.data);

// --- Expansion analytics (where to open the next branch) ---
export const getExpansionOpportunities = () =>
  client.get("/owner/expansion-opportunities").then((r) => r.data);
