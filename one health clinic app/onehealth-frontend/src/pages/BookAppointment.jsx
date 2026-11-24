import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getClinics, getDoctorsAtClinic, getSlots, bookAppointment } from "../api/patient";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function BookAppointment() {
  const [clinics, setClinics] = useState([]);
  const [clinicId, setClinicId] = useState("");
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(todayStr());
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getClinics().then(setClinics).catch(() => setError("Couldn't load branches."));
  }, []);

  useEffect(() => {
    setDoctorId("");
    setSlots([]);
    if (clinicId) {
      getDoctorsAtClinic(clinicId).then(setDoctors).catch(() => setError("Couldn't load doctors for this branch."));
    } else {
      setDoctors([]);
    }
  }, [clinicId]);

  useEffect(() => {
    setSelectedSlot(null);
    if (doctorId && clinicId && date) {
      getSlots(doctorId, clinicId, date).then(setSlots).catch(() => setError("Couldn't load slots."));
    } else {
      setSlots([]);
    }
  }, [doctorId, clinicId, date]);

  async function handleBook() {
    if (!selectedSlot) return;
    setError("");
    setMessage("");
    setLoading(true);
    try {
      await bookAppointment(selectedSlot.slotId);
      setMessage("Appointment booked! You can see it under My Appointments.");
      setSlots((prev) => prev.map((s) => (s.slotId === selectedSlot.slotId ? { ...s, status: "BOOKED" } : s)));
      setSelectedSlot(null);
    } catch (err) {
      setError(err.response?.data?.message || "That slot just got taken. Please pick another one.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <div className="section-header">
        <h2>Book an appointment</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <div className="filter-bar">
        <div className="field">
          <label>Branch</label>
          <select value={clinicId} onChange={(e) => setClinicId(e.target.value)}>
            <option value="">Select a branch</option>
            {clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName} - {c.city}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Doctor</label>
          <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)} disabled={!clinicId}>
            <option value="">Select a doctor</option>
            {doctors.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}{d.specializations?.length ? ` - ${d.specializations.map((s) => s.name).join(", ")}` : ""}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Date</label>
          <input type="date" value={date} min={todayStr()} onChange={(e) => setDate(e.target.value)} />
        </div>
      </div>

      {doctorId && (
        <div className="card">
          <h3>Available times</h3>
          {slots.length === 0 ? (
            <p className="empty-state">No slots for this date. Try another day.</p>
          ) : (
            <div className="slot-grid">
              {slots.map((s) => (
                <button
                  key={s.slotId}
                  className={
                    "slot-btn" +
                    (s.status !== "AVAILABLE" ? " booked" : selectedSlot?.slotId === s.slotId ? " selected" : "")
                  }
                  disabled={s.status !== "AVAILABLE"}
                  onClick={() => setSelectedSlot(s)}
                >
                  {s.startTime}
                </button>
              ))}
            </div>
          )}
          {selectedSlot && (
            <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={handleBook} disabled={loading}>
              {loading ? "Booking..." : `Confirm ${selectedSlot.startTime}`}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
