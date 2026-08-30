import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyAvailability, addAvailability, deleteAvailability, getMyClinics } from "../../api/doctor";
import { DOCTOR_NAV_ITEMS } from "./navItems";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

const emptyForm = { clinicId: "", dayOfWeek: "MONDAY", startTime: "09:00", endTime: "13:00", slotDurationMinutes: 15 };

/**
 * A doctor sets recurring weekly hours at one of their assigned branches. The
 * backend rejects any window overlapping hours they already have at ANY
 * branch on the same weekday - that's what stops one doctor being scheduled
 * at two locations at once. Branch is picked by name (from branches the owner
 * has actually assigned this doctor to) rather than a raw numeric ID.
 */
export default function DoctorAvailability() {
  const [availability, setAvailability] = useState([]);
  const [clinics, setClinics] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [loadingClinics, setLoadingClinics] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [resyncWarnings, setResyncWarnings] = useState([]);

  function load() {
    setLoading(true);
    getMyAvailability().then(setAvailability).catch(() => setError("Couldn't load your availability.")).finally(() => setLoading(false));
  }

  useEffect(load, []);
  useEffect(() => {
    getMyClinics().then(setClinics).catch(() => setError("Couldn't load your assigned branches.")).finally(() => setLoadingClinics(false));
  }, []);

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  function describeResync(resync, verb) {
    if (!resync) return `Availability window ${verb}.`;
    const parts = [];
    if (resync.slotsAdded > 0) parts.push(`${resync.slotsAdded} slot(s) added`);
    if (resync.slotsCancelled > 0) parts.push(`${resync.slotsCancelled} slot(s) removed`);
    const summary = parts.length > 0
      ? ` ${resync.futureDatesChecked} already-viewed future date(s) updated - ${parts.join(", ")}.`
      : resync.futureDatesChecked > 0
        ? ` Checked ${resync.futureDatesChecked} already-viewed future date(s) - no changes needed there.`
        : "";
    return `Availability window ${verb}.${summary}`;
  }

  async function handleAdd(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    setResyncWarnings([]);
    try {
      const res = await addAvailability({ ...form, slotDurationMinutes: Number(form.slotDurationMinutes) });
      setForm(emptyForm);
      setMessage(describeResync(res.resync, "added"));
      setResyncWarnings(res.resync?.warnings || []);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add that window.");
    }
  }

  async function handleDelete(id) {
    setError("");
    setMessage("");
    setResyncWarnings([]);
    try {
      const resync = await deleteAvailability(id);
      setMessage(describeResync(resync, "removed"));
      setResyncWarnings(resync?.warnings || []);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't remove that window.");
    }
  }

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>My weekly availability</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}
      {resyncWarnings.length > 0 && (
        <div className="form-error">
          {resyncWarnings.map((w, i) => <div key={i}>{w}</div>)}
        </div>
      )}

      <form className="form-card wide" onSubmit={handleAdd}>
        <h3>Add a recurring window</h3>
        <div className="field">
          <label>Branch</label>
          <select required value={form.clinicId} onChange={update("clinicId")} disabled={loadingClinics}>
            <option value="">{loadingClinics ? "Loading branches..." : "Select a branch"}</option>
            {clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName}</option>
            ))}
          </select>
          {!loadingClinics && clinics.length === 0 && (
            <p style={{ fontSize: "0.8rem", color: "var(--ink-soft)", marginTop: 4 }}>
              You're not assigned to any branch yet - ask the owner to assign you to one first.
            </p>
          )}
        </div>
        <div className="field">
          <label>Day of week</label>
          <select value={form.dayOfWeek} onChange={update("dayOfWeek")}>
            {DAYS.map((d) => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Start time</label>
          <input required type="time" value={form.startTime} onChange={update("startTime")} />
        </div>
        <div className="field">
          <label>End time</label>
          <input required type="time" value={form.endTime} onChange={update("endTime")} />
        </div>
        <div className="field">
          <label>Slot length (minutes)</label>
          <input required type="number" min="5" value={form.slotDurationMinutes} onChange={update("slotDurationMinutes")} />
        </div>
        <button className="btn btn-primary" type="submit">Add window</button>
      </form>

      <div className="section-header">
        <h3>Current schedule</h3>
      </div>
      {loading ? (
        <p>Loading...</p>
      ) : availability.length === 0 ? (
        <p className="empty-state">No availability set yet.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Day</th><th>Time</th><th>Branch</th><th>Slot length</th><th></th></tr>
            </thead>
            <tbody>
              {availability.map((a) => (
                <tr key={a.id}>
                  <td>{a.dayOfWeek}</td>
                  <td>{a.startTime} - {a.endTime}</td>
                  <td>{a.clinicName}</td>
                  <td>{a.slotDurationMinutes} min</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => handleDelete(a.id)}>Remove</button>
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
