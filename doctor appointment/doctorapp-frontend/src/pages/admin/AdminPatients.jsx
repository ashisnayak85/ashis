import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getPatients } from "../../api/admin";
import { ADMIN_NAV_ITEMS } from "./navItems";

export default function AdminPatients() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getPatients()
      .then(setPatients)
      .catch(() => setError("Couldn't load patients."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout title="Admin Portal" navItems={ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Patients</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : patients.length === 0 ? (
        <div className="empty-state">No patients have registered yet.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Gender</th>
                <th>Date of birth</th>
              </tr>
            </thead>
            <tbody>
              {patients.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.email}</td>
                  <td>{p.phone || "—"}</td>
                  <td>{p.gender || "—"}</td>
                  <td>{p.dob || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardLayout>
  );
}
