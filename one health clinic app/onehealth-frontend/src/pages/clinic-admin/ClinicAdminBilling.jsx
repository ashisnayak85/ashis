import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getPendingPrescriptions, billPrescription } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

/**
 * The billing queue: prescriptions the doctor has written for this branch
 * that haven't been billed yet. Billing is the step that actually charges
 * the patient and deducts stock - the doctor's prescription itself carries
 * no price at all (see DoctorAppointments' prescribe modal).
 *
 * The admin can bill LESS than what was prescribed for any line (e.g. a
 * patient can only afford 6 of the 10 prescribed tablets) but never more -
 * stock is always tracked down to a single loose unit for exactly this
 * reason. Only the selling price/total is ever shown here - never cost or
 * profit, which stay owner-only.
 */
export default function ClinicAdminBilling() {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [active, setActive] = useState(null); // prescription being billed
  const [quantities, setQuantities] = useState({}); // medicineId -> quantity string
  const [saving, setSaving] = useState(false);
  const [receipt, setReceipt] = useState(null);

  function load() {
    setLoading(true);
    getPendingPrescriptions()
      .then(setPending)
      .catch(() => setError("Couldn't load pending prescriptions."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function openBilling(prescription) {
    setActive(prescription);
    setReceipt(null);
    const initial = {};
    prescription.items.forEach((it) => {
      initial[it.medicineId] = String(it.prescribedQuantity);
    });
    setQuantities(initial);
  }

  async function submitBill() {
    setError("");
    setSaving(true);
    try {
      const items = active.items
        .map((it) => ({ medicineId: it.medicineId, quantity: Number(quantities[it.medicineId] || 0) }))
        .filter((it) => it.quantity > 0);
      if (items.length === 0) {
        setError("Bill at least one medicine, or the patient isn't buying anything today.");
        setSaving(false);
        return;
      }
      const result = await billPrescription(active.id, { items });
      setReceipt(result);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't create the bill.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <DashboardLayout title="Clinic Admin Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Billing</h2>
      </div>
      {error && <div className="form-error">{error}</div>}

      {!active && (
        loading ? (
          <p className="empty-state">Loading...</p>
        ) : pending.length === 0 ? (
          <p className="empty-state">Nothing waiting to be billed right now.</p>
        ) : (
          <div className="card" style={{ overflowX: "auto" }}>
            <table className="data-table">
              <thead>
                <tr><th>Patient</th><th>Doctor</th><th>Prescribed</th><th>Written</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {pending.map((p) => (
                  <tr key={p.id}>
                    <td>{p.patientName}</td>
                    <td>{p.doctorName}</td>
                    <td>{p.items.map((it) => `${it.medicineName} x${it.prescribedQuantity}`).join(", ")}</td>
                    <td>{new Date(p.createdAt).toLocaleString()}</td>
                    <td className="actions">
                      <button className="btn btn-primary btn-sm" onClick={() => openBilling(p)}>Bill</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {active && !receipt && (
        <div className="card" style={{ maxWidth: 640 }}>
          <h3>Bill for {active.patientName}</h3>
          <p className="field-hint">
            Adjust quantity down if the patient is only buying part of what was prescribed - stock is tracked
            down to a single loose unit, so this doesn't have to be a full pack/strip.
          </p>
          <table className="data-table">
            <thead>
              <tr><th>Medicine</th><th>Prescribed</th><th>Bill quantity</th></tr>
            </thead>
            <tbody>
              {active.items.map((it) => (
                <tr key={it.medicineId}>
                  <td>{it.medicineName} <span className="text-muted">({it.dosageInstructions})</span></td>
                  <td>{it.prescribedQuantity} {it.unit}(s)</td>
                  <td>
                    <input
                      type="number" min="0" max={it.prescribedQuantity} style={{ width: 90 }}
                      value={quantities[it.medicineId] ?? ""}
                      onChange={(e) => setQuantities((q) => ({ ...q, [it.medicineId]: e.target.value }))}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="actions" style={{ marginTop: 16 }}>
            <button className="btn btn-primary" disabled={saving} onClick={submitBill}>
              {saving ? "Billing..." : "Create bill"}
            </button>
            <button className="btn btn-secondary" onClick={() => setActive(null)}>Cancel</button>
          </div>
        </div>
      )}

      {receipt && (
        <div className="card" style={{ maxWidth: 640 }}>
          <h3>Receipt</h3>
          <table className="data-table">
            <thead>
              <tr><th>Medicine</th><th>Qty</th><th>Price/unit</th><th>Line total</th></tr>
            </thead>
            <tbody>
              {receipt.items.map((it) => (
                <tr key={it.medicineId}>
                  <td>{it.medicineName}</td>
                  <td>{it.quantityBilled} {it.unit}(s)</td>
                  <td>₹{it.sellingPricePerUnit}</td>
                  <td>₹{it.lineTotal}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p style={{ fontSize: "1.1rem", marginTop: 12 }}><strong>Total: ₹{receipt.totalAmount}</strong></p>
          <button className="btn btn-secondary" onClick={() => { setActive(null); setReceipt(null); }}>
            Done
          </button>
        </div>
      )}
    </DashboardLayout>
  );
}
