import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getClinicAvailability, setAvailabilityActive, getAvailabilityHistory } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

/**
 * Every doctor's weekly time period at this branch, each showing the time
 * window, doctor name, and branch name, with a single on/off button - exactly
 * the requirement: "turn off online booking if the doctor can't make it,
 * without deleting the schedule". Defaults to on (see DoctorAvailability.active
 * default on the backend). A "Change history" tab shows who did what and when,
 * since both the owner and clinic authority can flip this switch.
 */
export default function ClinicAdminAvailability() {
  const [tab, setTab] = useState("periods");
  const [periods, setPeriods] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  function loadPeriods() {
    setLoading(true);
    getClinicAvailability()
      .then(setPeriods)
      .catch(() => setError("Couldn't load time periods."))
      .finally(() => setLoading(false));
  }

  useEffect(loadPeriods, []);

  function openHistory() {
    setTab("history");
    setLoadingHistory(true);
    getAvailabilityHistory()
      .then(setHistory)
      .catch(() => setError("Couldn't load change history."))
      .finally(() => setLoadingHistory(false));
  }

  async function handleToggle(period) {
    setError("");
    setMessage("");
    try {
      await setAvailabilityActive(period.id, !period.active);
      setMessage(
        `${period.doctorName}'s ${period.dayOfWeek} ${period.startTime}-${period.endTime} slot is now ${!period.active ? "on" : "off"}.`
      );
      loadPeriods();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that time period.");
    }
  }

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Time Periods</h2>
      </div>
      <p style={{ color: "var(--ink-soft)", marginTop: -8 }}>
        Every doctor's recurring weekly time window at this branch. Turn one off if the doctor can't
        make it - existing bookings are never auto-cancelled, but new online bookings stop immediately.
      </p>

      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        <button className={`btn btn-sm ${tab === "periods" ? "btn-primary" : "btn-secondary"}`} onClick={() => setTab("periods")}>
          Time periods
        </button>
        <button className={`btn btn-sm ${tab === "history" ? "btn-primary" : "btn-secondary"}`} onClick={openHistory}>
          Change history
        </button>
      </div>

      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      {tab === "periods" ? (
        loading ? (
          <p>Loading...</p>
        ) : periods.length === 0 ? (
          <p className="empty-state">No doctors have set up a time period at this branch yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr><th>Doctor</th><th>Day</th><th>Time</th><th>Branch</th><th>Slot length</th><th>Status</th><th></th></tr>
              </thead>
              <tbody>
                {periods.map((p) => (
                  <tr key={p.id}>
                    <td>{p.doctorName}</td>
                    <td>{p.dayOfWeek}</td>
                    <td>{p.startTime} - {p.endTime}</td>
                    <td>{p.clinicName}</td>
                    <td>{p.slotDurationMinutes} min</td>
                    <td>
                      <span className={`badge ${p.active ? "badge-success" : "badge-muted"}`}>
                        {p.active ? "On" : "Off"}
                      </span>
                    </td>
                    <td className="actions">
                      <button className="btn btn-secondary btn-sm" onClick={() => handleToggle(p)}>
                        Turn {p.active ? "off" : "on"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : loadingHistory ? (
        <p>Loading...</p>
      ) : history.length === 0 ? (
        <p className="empty-state">No changes recorded yet.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>When</th><th>Who</th><th>Action</th><th>Doctor</th><th>Day / Time</th><th>Note</th></tr>
            </thead>
            <tbody>
              {history.map((h) => (
                <tr key={h.id}>
                  <td>{new Date(h.performedAt).toLocaleString()}</td>
                  <td>{h.performedByName} <span style={{ color: "var(--ink-soft)" }}>({h.performedByRole === "CLINIC_ADMIN" ? "Clinic Authority" : h.performedByRole})</span></td>
                  <td>{h.action}</td>
                  <td>{h.doctorName}</td>
                  <td>{h.dayOfWeek} {h.startTime}-{h.endTime}</td>
                  <td>{h.note || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardLayout>
  );
}
