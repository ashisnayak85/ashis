import client from "./client";

export const findNearbyDoctors = ({ lat, lng, radiusKm, specialization }) =>
  client
    .get("/doctors/nearby", { params: { lat, lng, radiusKm, specialization } })
    .then((r) => r.data);

export const getDoctorProfile = (doctorId) =>
  client.get(`/doctors/${doctorId}/profile`).then((r) => r.data);

export const getDoctorSlots = (doctorId, clinicId, date) =>
  client
    .get(`/doctors/${doctorId}/slots`, { params: { clinicId, date } })
    .then((r) => r.data);

export const getDoctorRatings = (doctorId, page = 0, size = 10) =>
  client
    .get(`/doctors/${doctorId}/ratings`, { params: { page, size } })
    .then((r) => r.data);
