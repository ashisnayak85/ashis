import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getAllAppointments } from "../../api/admin";
import { ADMIN_NAV_ITEMS } from "./navItems";

const FILTERS = ["ALL", "BOOKED", "COMPLETED", "CANCELLED", "NO_SHOW"];

function badgeClass(status) {
  if (status === "BOOKED") return "badge-success";
  if (status === "COMPLETED") return "badge-muted";
  if (status === "CANCELLED" || status === "NO_SHOW") return "badge-danger";
  return "badge-muted";
}

export default function AdminAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getAllAppointments()
      .then(setAppointments)
      .catch(() => setError("Couldn't load appointments."))
      .finally(() => setLoading(false));
  }, []);

  const visible = filter === "ALL" ? appointments : appointments.filter((a) => a.status === filter);

  return (
    <DashboardLayout title="Admin Portal" navItems={ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Appointments</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="tabs">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            className={`tab-btn ${filter === f ? "active" : ""}`}
            onClick={() => setFilter(f)}
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
                <th>Doctor</th>
                <th>Clinic</th>
                <th>Date & time</th>
                <th>Fee</th>
                <th>Payment</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((a) => (
                <tr key={a.id}>
                  <td>{a.patientName}</td>
                  <td>{a.doctorName}</td>
                  <td>{a.clinicName}</td>
                  <td>
                    {a.appointmentDate}
                    <br />
                    {a.startTime}&ndash;{a.endTime}
                  </td>
                  <td>{a.consultationFee != null ? `Rs. ${a.consultationFee}` : "—"}</td>
                  <td>
                    <span className="badge badge-muted">{a.paymentStatus}</span>
                  </td>
                  <td>
                    <span className={`badge ${badgeClass(a.status)}`}>{a.status}</span>
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
