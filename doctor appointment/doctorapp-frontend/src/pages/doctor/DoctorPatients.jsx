import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyPatients } from "../../api/doctorDashboard";
import { DOCTOR_NAV_ITEMS } from "./navItems";

function badgeClass(status) {
  if (status === "BOOKED") return "badge-success";
  if (status === "COMPLETED") return "badge-muted";
  if (status === "CANCELLED" || status === "NO_SHOW") return "badge-danger";
  return "badge-muted";
}

export default function DoctorPatients() {
  const [patients, setPatients] = useState([]);
  const [expandedId, setExpandedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getMyPatients()
      .then(setPatients)
      .catch(() => setError("Couldn't load your patients."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>Patients</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : patients.length === 0 ? (
        <div className="empty-state">No patients have booked with you yet.</div>
      ) : (
        patients.map((p) => {
          const isOpen = expandedId === p.patientId;
          return (
            <div className="card" key={p.patientId} style={{ marginBottom: 14 }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  flexWrap: "wrap",
                  gap: 10,
                  cursor: "pointer",
                }}
                onClick={() => setExpandedId(isOpen ? null : p.patientId)}
              >
                <div>
                  <h3 style={{ marginBottom: 4 }}>{p.patientName}</h3>
                  {p.patientPhone && <p style={{ margin: 0 }}>{p.patientPhone}</p>}
                </div>
                <span className="badge badge-muted">
                  {p.totalVisits} visit{p.totalVisits === 1 ? "" : "s"}
                </span>
              </div>

              {isOpen && (
                <div className="table-wrap" style={{ marginTop: 16, boxShadow: "none" }}>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Clinic</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {p.appointments.map((a) => (
                        <tr key={a.id}>
                          <td>{a.appointmentDate}</td>
                          <td>
                            {a.startTime}&ndash;{a.endTime}
                          </td>
                          <td>{a.clinicName}</td>
                          <td>
                            <span className={`badge ${badgeClass(a.status)}`}>{a.status}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          );
        })
      )}
    </DashboardLayout>
  );
}
