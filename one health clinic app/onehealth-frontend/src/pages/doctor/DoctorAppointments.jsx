import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  getMyAppointments,
  updateAppointmentStatus,
  getAvailableMedicines,
  createPrescription,
  getPrescription,
} from "../../api/doctor";
import { DOCTOR_NAV_ITEMS } from "./navItems";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

/** One line in the prescribe form: a medicine + quantity + optional dosage note. */
function emptyLine() {
  return { medicineId: "", quantity: 1, dosageInstructions: "" };
}

function PrescribeModal({ appointment, onClose, onSaved }) {
  const [medicines, setMedicines] = useState([]);
  const [lines, setLines] = useState([emptyLine()]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [existing, setExisting] = useState(null);

  useEffect(() => {
    Promise.all([
      getAvailableMedicines(appointment.id),
      getPrescription(appointment.id).catch(() => null),
    ])
      .then(([meds, presc]) => {
        setMedicines(meds);
        setExisting(presc);
      })
      .catch(() => setError("Couldn't load this branch's stock."))
      .finally(() => setLoading(false));
  }, [appointment.id]);

  function updateLine(index, field, value) {
    setLines((ls) => ls.map((l, i) => (i === index ? { ...l, [field]: value } : l)));
  }

  function addLine() {
    setLines((ls) => [...ls, emptyLine()]);
  }

  function removeLine(index) {
    setLines((ls) => ls.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    const items = lines
      .filter((l) => l.medicineId)
      .map((l) => ({
        medicineId: Number(l.medicineId),
        quantity: Number(l.quantity) || 1,
        dosageInstructions: l.dosageInstructions || undefined,
      }));
    if (items.length === 0) {
      setError("Add at least one medicine.");
      return;
    }
    setSaving(true);
    try {
      await createPrescription(appointment.id, { items });
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't save this prescription - check stock levels.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="form-card wide" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 640, margin: "40px auto" }}>
        <h3>Prescribe - {appointment.patientName}</h3>
        {error && <div className="form-error">{error}</div>}

        {loading ? (
          <p className="empty-state">Loading stock...</p>
        ) : existing ? (
          <div>
            <p>
              A prescription already exists for this appointment
              {existing.status === "BILLED" ? " and has been billed at the counter." : " - waiting to be billed at the counter."}
            </p>
            <table className="data-table">
              <thead><tr><th>Medicine</th><th>Qty</th><th>Dosage</th></tr></thead>
              <tbody>
                {existing.items.map((it, i) => (
                  <tr key={i}><td>{it.medicineName}</td><td>{it.prescribedQuantity} {it.unit}(s)</td><td>{it.dosageInstructions}</td></tr>
                ))}
              </tbody>
            </table>
            <button className="btn btn-secondary" onClick={onClose}>Close</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            {lines.map((line, i) => (
              <div key={i} className="field-row" style={{ alignItems: "flex-end" }}>
                <div className="field" style={{ flex: 2 }}>
                  <label>Medicine</label>
                  <select value={line.medicineId} onChange={(e) => updateLine(i, "medicineId", e.target.value)}>
                    <option value="">Select...</option>
                    {medicines.map((m) => (
                      <option key={m.medicineId} value={m.medicineId} disabled={m.quantityOnHand <= 0}>
                        {m.medicineName} ({m.quantityOnHand} {m.unit}(s) in stock, ₹{m.sellingPrice ?? "-"})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field" style={{ flex: 1 }}>
                  <label>Qty</label>
                  <input type="number" min="1" value={line.quantity} onChange={(e) => updateLine(i, "quantity", e.target.value)} />
                </div>
                <div className="field" style={{ flex: 2 }}>
                  <label>Dosage note</label>
                  <input
                    placeholder="e.g. 1-0-1 after food"
                    value={line.dosageInstructions}
                    onChange={(e) => updateLine(i, "dosageInstructions", e.target.value)}
                  />
                </div>
                {lines.length > 1 && (
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => removeLine(i)}>Remove</button>
                )}
              </div>
            ))}
            <button type="button" className="btn btn-secondary btn-sm" onClick={addLine} style={{ marginBottom: 16 }}>
              + Add another medicine
            </button>
            <div style={{ display: "flex", gap: 10 }}>
              <button className="btn btn-primary" disabled={saving} type="submit">
                {saving ? "Saving..." : "Save prescription"}
              </button>
              <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default function DoctorAppointments() {
  const [date, setDate] = useState(todayStr());
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [prescribing, setPrescribing] = useState(null); // appointment being prescribed for

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
                    {(a.status === "COMPLETED" || a.status === "BOOKED") && (
                      <button className="btn btn-primary btn-sm" onClick={() => setPrescribing(a)}>Prescribe</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {prescribing && (
        <PrescribeModal
          appointment={prescribing}
          onClose={() => setPrescribing(null)}
          onSaved={() => {
            setPrescribing(null);
            load();
          }}
        />
      )}
    </DashboardLayout>
  );
}
