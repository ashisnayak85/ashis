import { useEffect, useState } from "react";
import { getMyAppointments, cancelAppointment } from "../api/patient";

export default function MyAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function load() {
    setLoading(true);
    getMyAppointments()
      .then(setAppointments)
      .catch(() => setError("Couldn't load your appointments."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCancel(id) {
    try {
      await cancelAppointment(id);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't cancel that appointment.");
    }
  }

  return (
    <div className="container">
      <div className="section-header">
        <h2>My appointments</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {loading ? (
        <p>Loading...</p>
      ) : appointments.length === 0 ? (
        <p className="empty-state">No appointments yet.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th><th>Time</th><th>Doctor</th><th>Branch</th><th>Status</th><th></th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((a) => (
                <tr key={a.id}>
                  <td>{a.appointmentDate}</td>
                  <td>{a.startTime}</td>
                  <td>{a.doctorName}</td>
                  <td>{a.clinicName}</td>
                  <td><span className={`status-pill status-${a.status}`}>{a.status}</span></td>
                  <td className="actions">
                    {a.status === "BOOKED" && (
                      <button className="btn btn-secondary btn-sm" onClick={() => handleCancel(a.id)}>Cancel</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
