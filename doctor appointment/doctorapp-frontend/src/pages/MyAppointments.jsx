import { useEffect, useState } from "react";
import { getMyAppointments, cancelAppointment } from "../api/appointments";

export default function MyAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getMyAppointments();
      setAppointments(data);
    } catch (err) {
      setError("Couldn't load your appointments.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(id) {
    try {
      await cancelAppointment(id);
      load();
    } catch (err) {
      setError("Couldn't cancel that appointment. Please try again.");
    }
  }

  return (
    <div className="container" style={{ paddingTop: 32, paddingBottom: 40 }}>
      <h2>My appointments</h2>
      {error && <div className="form-error">{error}</div>}
      {loading && <p>Loading...</p>}
      {!loading && appointments.length === 0 && (
        <div className="empty-state">You haven't booked any appointments yet.</div>
      )}
      {appointments.map((a) => (
        <div className="card" key={a.id} style={{ marginBottom: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", flexWrap: "wrap", gap: 10 }}>
            <div>
              <h3 style={{ marginBottom: 4 }}>{a.doctorName}</h3>
              <p style={{ margin: 0 }}>{a.clinicName} — {a.clinicAddress}</p>
              <p style={{ margin: 0 }}>{a.appointmentDate} · {a.startTime}–{a.endTime}</p>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
              <span className={`status-pill status-${a.status}`}>{a.status}</span>
              {a.status === "BOOKED" && (
                <button className="btn btn-secondary" onClick={() => handleCancel(a.id)}>
                  Cancel
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
