import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyClinicDoctors, getSlots, bookWalkIn } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Front-desk booking for a patient who walked in without a prior online
 * booking. Doctor is picked by name from this branch's roster - previously
 * this asked for a raw doctor ID, which nobody at the front desk would ever
 * know; now it's a dropdown backed by GET /api/clinic-admin/doctors.
 */
export default function ClinicAdminWalkIn() {
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(todayStr());
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [patientName, setPatientName] = useState("");
  const [patientPhone, setPatientPhone] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingDoctors, setLoadingDoctors] = useState(true);

  useEffect(() => {
    getMyClinicDoctors()
      .then(setDoctors)
      .catch(() => setError("Couldn't load this branch's doctors."))
      .finally(() => setLoadingDoctors(false));
  }, []);

  useEffect(() => {
    setSelectedSlot(null);
    if (doctorId && date) {
      getSlots(doctorId, date).then(setSlots).catch(() => setError("Couldn't load slots for that doctor."));
    } else {
      setSlots([]);
    }
  }, [doctorId, date]);

  async function handleBook(e) {
    e.preventDefault();
    if (!selectedSlot) {
      setError("Pick a time slot first.");
      return;
    }
    setError("");
    setMessage("");
    setLoading(true);
    try {
      await bookWalkIn({ slotId: selectedSlot.slotId, patientName, patientPhone });
      setMessage(`Booked ${patientName} for ${selectedSlot.startTime}.`);
      setPatientName("");
      setPatientPhone("");
      setSelectedSlot(null);
      setSlots((prev) => prev.map((s) => (s.slotId === selectedSlot.slotId ? { ...s, status: "BOOKED" } : s)));
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't book that slot. It may have just been taken.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Book a walk-in</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <div className="filter-bar">
        <div className="field">
          <label>Doctor</label>
          <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)} disabled={loadingDoctors}>
            <option value="">{loadingDoctors ? "Loading doctors..." : "Select a doctor"}</option>
            {doctors.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}{d.specializations?.length ? ` - ${d.specializations.map((s) => s.name).join(", ")}` : ""}
              </option>
            ))}
          </select>
          {!loadingDoctors && doctors.length === 0 && (
            <p style={{ fontSize: "0.8rem", color: "var(--ink-soft)", marginTop: 4 }}>
              No doctors are assigned to this branch yet - ask the owner to assign one.
            </p>
          )}
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
            <p className="empty-state">No slots for this date/doctor.</p>
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
        </div>
      )}

      {selectedSlot && (
        <form className="form-card wide" onSubmit={handleBook} style={{ marginTop: 20 }}>
          <h3>Patient details - {selectedSlot.startTime}</h3>
          <div className="field">
            <label>Patient name</label>
            <input required value={patientName} onChange={(e) => setPatientName(e.target.value)} />
          </div>
          <div className="field">
            <label>Phone</label>
            <input required value={patientPhone} onChange={(e) => setPatientPhone(e.target.value)} />
          </div>
          <button className="btn btn-primary" disabled={loading} type="submit">
            {loading ? "Booking..." : "Confirm walk-in booking"}
          </button>
        </form>
      )}
    </DashboardLayout>
  );
}
