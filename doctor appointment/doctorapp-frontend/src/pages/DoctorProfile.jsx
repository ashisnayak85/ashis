import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getDoctorProfile, getDoctorSlots } from "../api/doctors";
import { bookAppointment } from "../api/appointments";
import { useAuth } from "../context/AuthContext";

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

export default function DoctorProfile() {
  const { doctorId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [clinicId, setClinicId] = useState(null);
  const [date, setDate] = useState(todayISO());
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [error, setError] = useState("");
  const [booking, setBooking] = useState(false);
  const [confirmation, setConfirmation] = useState(null);

  useEffect(() => {
    getDoctorProfile(doctorId).then((data) => {
      setProfile(data);
      if (data.clinics?.length) setClinicId(data.clinics[0].id);
    });
  }, [doctorId]);

  useEffect(() => {
    if (clinicId && date) loadSlots();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, date]);

  async function loadSlots() {
    setError("");
    setSelectedSlot(null);
    try {
      const data = await getDoctorSlots(doctorId, clinicId, date);
      setSlots(data);
    } catch (err) {
      setError("Couldn't load availability for that date.");
    }
  }

  async function handleBook() {
    if (!user) {
      navigate("/login");
      return;
    }
    if (!selectedSlot) return;
    setBooking(true);
    setError("");
    try {
      const appointment = await bookAppointment(selectedSlot.slotId);
      setConfirmation(appointment);
      loadSlots();
    } catch (err) {
      setError(err.response?.data?.message || "That slot was just taken. Please pick another.");
      loadSlots();
    } finally {
      setBooking(false);
    }
  }

  if (!profile) return <div className="container"><p>Loading doctor profile...</p></div>;

  if (confirmation) {
    return (
      <div className="container">
        <div className="card" style={{ maxWidth: 480, margin: "40px auto", textAlign: "center" }}>
          <h2>Appointment confirmed</h2>
          <p>
            {confirmation.doctorName} · {confirmation.clinicName}<br />
            {confirmation.appointmentDate} at {confirmation.startTime}
          </p>
          <button className="btn btn-primary" onClick={() => navigate("/my-appointments")}>
            View my appointments
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="card" style={{ marginTop: 24 }}>
        <div style={{ display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}>
          <div className="doctor-avatar" style={{ width: 72, height: 72, fontSize: "1.5rem" }}>
            {profile.name?.[0]}
          </div>
          <div>
            <h2 style={{ marginBottom: 4 }}>{profile.name}</h2>
            <p style={{ margin: 0 }}>{profile.qualification} · {profile.experienceYears ?? 0} yrs experience</p>
            <p style={{ margin: 0 }}>{profile.specializations?.join(", ")}</p>
            {profile.consultationFee != null && <p style={{ margin: 0 }}>₹{profile.consultationFee} consultation fee</p>}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 20 }}>
        <h3>Select clinic & date</h3>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 12 }}>
          <select value={clinicId ?? ""} onChange={(e) => setClinicId(Number(e.target.value))}>
            {profile.clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName} — {c.address}</option>
            ))}
          </select>
          <input type="date" min={todayISO()} value={date} onChange={(e) => setDate(e.target.value)} />
        </div>

        {error && <div className="form-error" style={{ marginTop: 14 }}>{error}</div>}

        <div className="slot-grid">
          {slots.length === 0 && <p>No slots available for this date.</p>}
          {slots.map((s) => {
            const isBooked = s.status !== "AVAILABLE";
            const isSelected = selectedSlot?.slotId === s.slotId;
            return (
              <button
                key={s.slotId}
                type="button"
                disabled={isBooked}
                className={`slot-btn ${isBooked ? "booked" : ""} ${isSelected ? "selected" : ""}`}
                onClick={() => setSelectedSlot(s)}
              >
                {s.startTime}
              </button>
            );
          })}
        </div>

        <button
          className="btn btn-primary"
          style={{ marginTop: 20 }}
          disabled={!selectedSlot || booking}
          onClick={handleBook}
        >
          {booking ? "Booking..." : user ? "Confirm appointment" : "Log in to book"}
        </button>
      </div>
    </div>
  );
}
