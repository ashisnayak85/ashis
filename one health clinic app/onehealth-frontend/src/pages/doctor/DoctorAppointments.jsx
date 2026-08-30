import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyAppointments, updateAppointmentStatus } from "../../api/doctor";
import { DOCTOR_NAV_ITEMS } from "./navItems";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function DoctorAppointments() {
  const [date, setDate] = useState(todayStr());
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function load() {
    setLoading(true);
    getMyAppointments(date)
      .then(setAppointments)
      .catch(() => setError("Couldn't load appointments."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [date]);

  async function handleStatus(id, status) {
    try {
      await updateAppointmentStatus(id, status);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that appointment.");
    }
  }

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>My appointments</h2>
      </div>
      {error && <div className="form-error">{error}</div>}

      <div className="filter-bar">
        <div className="field">
          <label>Date</label>
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : appointments.length === 0 ? (
        <p className="empty-state">No appointments on this date.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Time</th><th>Patient</th><th>Branch</th><th>Source</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {appointments.map((a) => (
                <tr key={a.id}>
                  <td>{a.startTime}</td>
                  <td>{a.patientName}</td>
                  <td>{a.clinicName}</td>
                  <td><span className={`source-pill source-${a.source}`}>{a.source}</span></td>
                  <td><span className={`status-pill status-${a.status}`}>{a.status}</span></td>
                  <td className="actions">
                    {a.status === "BOOKED" && (
                      <>
                        <button className="btn btn-secondary btn-sm" onClick={() => handleStatus(a.id, "COMPLETED")}>Completed</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => handleStatus(a.id, "NO_SHOW")}>No-show</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardLayout>
  );
}
