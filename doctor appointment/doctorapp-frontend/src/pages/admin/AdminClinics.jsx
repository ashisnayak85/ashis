import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getClinics, verifyClinic, setClinicStatus } from "../../api/admin";
import { ADMIN_NAV_ITEMS } from "./navItems";

const FILTERS = [
  { key: "ALL", label: "All", verified: undefined },
  { key: "PENDING", label: "Pending verification", verified: false },
  { key: "VERIFIED", label: "Verified", verified: true },
];

export default function AdminClinics() {
  const [filter, setFilter] = useState("ALL");
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  function load(verified) {
    setLoading(true);
    setError("");
    getClinics(verified)
      .then(setClinics)
      .catch(() => setError("Couldn't load clinics."))
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
      await verifyClinic(id);
      const active = FILTERS.find((f) => f.key === filter);
      load(active.verified);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't verify that clinic.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleToggleActive(id, currentlyActive) {
    setBusyId(id);
    setError("");
    try {
      await setClinicStatus(id, !currentlyActive);
      const active = FILTERS.find((f) => f.key === filter);
      load(active.verified);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that clinic's status.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <DashboardLayout title="Admin Portal" navItems={ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Clinics</h2>
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
      ) : clinics.length === 0 ? (
        <div className="empty-state">No clinics in this view.</div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Clinic</th>
                <th>Address</th>
                <th>Doctors</th>
                <th>Verification</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {clinics.map((c) => (
                <tr key={c.id}>
                  <td>
                    <strong>{c.clinicName}</strong>
                    <br />
                    <span style={{ color: "var(--ink-soft)", fontSize: "0.85rem" }}>{c.phone || "—"}</span>
                  </td>
                  <td>
                    {c.address}
                    {c.city ? `, ${c.city}` : ""}
                  </td>
                  <td>{c.doctorCount}</td>
                  <td>
                    <span className={`badge ${c.verified ? "badge-success" : "badge-warning"}`}>
                      {c.verified ? "Verified" : "Pending"}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${c.active ? "badge-success" : "badge-danger"}`}>
                      {c.active ? "Active" : "Deactivated"}
                    </span>
                  </td>
                  <td className="actions">
                    {!c.verified && (
                      <button
                        className="btn btn-primary btn-sm"
                        disabled={busyId === c.id}
                        onClick={() => handleVerify(c.id)}
                      >
                        Verify
                      </button>
                    )}
                    <button
                      className="btn btn-secondary btn-sm"
                      disabled={busyId === c.id}
                      onClick={() => handleToggleActive(c.id, c.active)}
                    >
                      {c.active ? "Deactivate" : "Reactivate"}
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
