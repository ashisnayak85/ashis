import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/DashboardLayout";
import { getEmployees } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

/**
 * Staff roster - both ClinicAdmin and Doctor accounts, since both are
 * employees of the organization. Salary shown here is owner-only (this whole
 * page sits behind /owner/** which requires ROLE_OWNER).
 */
export default function OwnerEmployees() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getEmployees().then(setEmployees).catch(() => setError("Couldn't load staff.")).finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Staff / HR</h2>
      </div>
      <p style={{ color: "var(--ink-soft)", marginTop: -8 }}>
        Clinic authorities and doctors are both employees of the organization - manage their
        HR details and salary history here. Only you (the owner) can see this page.
      </p>
      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : employees.length === 0 ? (
        <p className="empty-state">No staff yet - add clinic authorities or doctors first.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Name</th><th>Role</th><th>Branch(es)</th><th>HR profile</th><th>Current salary</th><th></th></tr>
            </thead>
            <tbody>
              {employees.map((e) => (
                <tr key={e.userId}>
                  <td>{e.name}</td>
                  <td>{e.role === "CLINIC_ADMIN" ? "Clinic authority" : "Doctor"}</td>
                  <td>{e.clinicSummary}</td>
                  <td>
                    <span className={`badge ${e.profileComplete ? "badge-success" : "badge-warning"}`}>
                      {e.profileComplete ? "On file" : "Not set up"}
                    </span>
                  </td>
                  <td>{e.currentSalary != null ? `₹${Number(e.currentSalary).toLocaleString()}` : "-"}</td>
                  <td className="actions">
                    <Link className="btn btn-secondary btn-sm" to={`/owner/employees/${e.userId}`}>Manage</Link>
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
