import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getDoctors, verifyDoctor, setDoctorStatus } from "../../api/admin";
import { ADMIN_NAV_ITEMS } from "./navItems";

const FILTERS = [
  { key: "ALL", label: "All", verified: undefined },
  { key: "PENDING", label: "Pending verification", verified: false },
  { key: "VERIFIED", label: "Verified", verified: true },
];

export default function AdminDoctors() {
  const [filter, setFilter] = useState("ALL");
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load(verified) {
    setLoading(true);
    setError("");
    getDoctors(verified)
      .then(setDoctors)
      .catch(() => setError("Couldn't load doctors."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    const active = FILTERS.find((f) => f.key === filter);
    load(active.verified);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  async function handleVerify(id) {
    setBusyId(id);
    setError("");
    try {
      await verifyDoctor(id);
      const active = FILTERS.find((f) => f.key === filter);
      load(active.verified);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't verify that doctor.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleToggleActive(id, currentlyActive) {
    setBusyId(id);
    setError("");
    try {
      await setDoctorStatus(id, !currentlyActive);
      const active = FILTERS.find((f) => f.key === filter);
      load(active.verified);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that doctor's status.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <DashboardLayout title="Admin Portal" navItems={ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Doctors</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="tabs">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            type="button"
            className={`tab-btn ${filter === f.key ? "active" : ""}`}
            onClick={() => setFilter(f.key)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : doctors.length === 0 ? (
        <div className="empty-state">No doctors in this view.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Specializations</th>
                <th>Clinics</th>
                <th>Verification</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {doctors.map((d) => (
                <tr key={d.id}>
                  <td>
                    <strong>{d.name}</strong>
                    <br />
                    <span style={{ color: "var(--ink-soft)", fontSize: "0.85rem" }}>{d.email}</span>
                    <br />
                    <span style={{ color: "var(--ink-soft)", fontSize: "0.85rem" }}>
                      {d.qualification || "—"} {d.experienceYears ? `· ${d.experienceYears} yrs` : ""}
                    </span>
                  </td>
                  <td>{d.specializations?.length ? d.specializations.join(", ") : "—"}</td>
                  <td>{d.clinicCount}</td>
                  <td>
                    <span className={`badge ${d.verified ? "badge-success" : "badge-warning"}`}>
                      {d.verified ? "Verified" : "Pending"}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${d.active ? "badge-success" : "badge-danger"}`}>
                      {d.active ? "Active" : "Deactivated"}
                    </span>
                  </td>
                  <td className="actions">
                    {!d.verified && (
                      <button
                        className="btn btn-primary btn-sm"
                        disabled={busyId === d.id}
                        onClick={() => handleVerify(d.id)}
                      >
                        Verify
                      </button>
                    )}
                    <button
                      className="btn btn-secondary btn-sm"
                      disabled={busyId === d.id}
                      onClick={() => handleToggleActive(d.id, d.active)}
                    >
                      {d.active ? "Deactivate" : "Reactivate"}
                    </button>
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
