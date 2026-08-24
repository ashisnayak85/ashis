import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyDoctorAppointments, updateAppointmentStatus } from "../../api/doctorDashboard";
import { DOCTOR_NAV_ITEMS } from "./navItems";

const FILTERS = ["ALL", "BOOKED", "COMPLETED", "CANCELLED", "NO_SHOW"];

function badgeClass(status) {
  if (status === "BOOKED") return "badge-success";
  if (status === "COMPLETED") return "badge-muted";
  if (status === "CANCELLED" || status === "NO_SHOW") return "badge-danger";
  return "badge-muted";
}

export default function DoctorAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [filter, setFilter] = useState("BOOKED");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingId, setUpdatingId] = useState(null);

  function load() {
    setLoading(true);
    getMyDoctorAppointments()
      .then(setAppointments)
      .catch(() => setError("Couldn't load your appointments."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  async function handleStatusChange(id, status) {
    setUpdatingId(id);
    setError("");
    try {
      await updateAppointmentStatus(id, status);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that appointment.");
    } finally {
      setUpdatingId(null);
    }
  }

  const visible = filter === "ALL" ? appointments : appointments.filter((a) => a.status === filter);

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>Appointments</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="tabs">
        {FILTERS.map((f) => (
          <button
            key={f}
            className={`tab-btn ${filter === f ? "active" : ""}`}
            onClick={() => setFilter(f)}
            type="button"
          >
            {f === "ALL" ? "All" : f.replace("_", "-")}
          </button>
        ))}
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : visible.length === 0 ? (
        <div className="empty-state">No appointments in this view.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Patient</th>
                <th>Clinic</th>
                <th>Date & time</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((a) => (
                <tr key={a.id}>
                  <td>
                    {a.patientName}
                    {a.patientPhone && (
                      <>
                        <br />
                        <span style={{ color: "var(--ink-soft)", fontSize: "0.85rem" }}>{a.patientPhone}</span>
                      </>
                    )}
                  </td>
                  <td>{a.clinicName}</td>
                  <td>
                    {a.appointmentDate}
                    <br />
                    {a.startTime}&ndash;{a.endTime}
                  </td>
                  <td>
                    <span className={`badge ${badgeClass(a.status)}`}>{a.status}</span>
                  </td>
                  <td className="actions">
                    {a.status === "BOOKED" && (
                      <>
                        <button
                          className="btn btn-primary btn-sm"
                          disabled={updatingId === a.id}
                          onClick={() => handleStatusChange(a.id, "COMPLETED")}
                        >
                          Complete
                        </button>
                        <button
                          className="btn btn-secondary btn-sm"
                          disabled={updatingId === a.id}
                          onClick={() => handleStatusChange(a.id, "NO_SHOW")}
                        >
                          No-show
                        </button>
                        <button
                          className="btn btn-secondary btn-sm"
                          disabled={updatingId === a.id}
                          onClick={() => handleStatusChange(a.id, "CANCELLED")}
                        >
                          Cancel
                        </button>
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
