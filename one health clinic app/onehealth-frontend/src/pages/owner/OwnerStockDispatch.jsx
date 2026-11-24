import { useEffect, useMemo, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  getMedicines,
  getClinics,
  dispatchStock,
  getClinicStockForOwner,
  getStockReconciliation,
  updateMargin,
} from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

const EMPTY_FORM = { clinicId: "", medicineId: "", batchNumber: "", costPrice: "", mrp: "", quantity: "", expiryDate: "" };

/**
 * Owner-controlled: send (dispatch) medicine stock to a branch, set the
 * chargeable margin for each medicine at each branch, and see the
 * reconciliation report (dispatched vs. sold vs. what the system believes is
 * left). Clinics no longer add their own stock or set their own margin -
 * pricing authority is centralized here so a branch can't quietly overcharge
 * (or undercharge off-book) without the quantity-based reconciliation
 * catching it. See MedicineService for why.
 */
export default function OwnerStockDispatch() {
  const [medicines, setMedicines] = useState([]);
  const [clinics, setClinics] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const [selectedClinicId, setSelectedClinicId] = useState("");
  const [clinicStock, setClinicStock] = useState([]);
  const [loadingStock, setLoadingStock] = useState(false);
  const [editingMargin, setEditingMargin] = useState(null);
  const [marginDraft, setMarginDraft] = useState("");

  const [reconciliation, setReconciliation] = useState([]);
  const [loadingReconciliation, setLoadingReconciliation] = useState(true);

  useEffect(() => {
    Promise.all([getMedicines(), getClinics()]).then(([meds, cl]) => {
      setMedicines(meds.filter((m) => m.active));
      setClinics(cl.filter((c) => c.active));
    });
    loadReconciliation();
  }, []);

  const selectedMedicine = useMemo(
    () => medicines.find((m) => String(m.id) === String(form.medicineId)),
    [medicines, form.medicineId]
  );

  function loadReconciliation() {
    setLoadingReconciliation(true);
    getStockReconciliation()
      .then(setReconciliation)
      .catch(() => setError("Couldn't load the reconciliation report."))
      .finally(() => setLoadingReconciliation(false));
  }

  function loadClinicStock(clinicId) {
    if (!clinicId) {
      setClinicStock([]);
      return;
    }
    setLoadingStock(true);
    getClinicStockForOwner(clinicId)
      .then(setClinicStock)
      .catch(() => setError("Couldn't load that branch's stock."))
      .finally(() => setLoadingStock(false));
  }

  function updateField(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleDispatch(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    if (!form.clinicId || !form.medicineId) {
      setError("Pick a branch and a medicine.");
      return;
    }
    setSaving(true);
    try {
      const usesPacks = !!selectedMedicine?.packSize;
      await dispatchStock({
        clinicId: Number(form.clinicId),
        medicineId: Number(form.medicineId),
        batchNumber: form.batchNumber || undefined,
        costPrice: Number(form.costPrice),
        mrp: Number(form.mrp),
        packsReceived: usesPacks ? Number(form.quantity) : undefined,
        quantityReceived: usesPacks ? undefined : Number(form.quantity),
        expiryDate: form.expiryDate || undefined,
      });
      setMessage("Stock dispatched to the branch.");
      setForm((f) => ({ ...EMPTY_FORM, clinicId: f.clinicId })); // keep the same branch selected for convenience
      if (String(selectedClinicId) === String(form.clinicId)) loadClinicStock(form.clinicId);
      loadReconciliation();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't dispatch that stock.");
    } finally {
      setSaving(false);
    }
  }

  function startEditMargin(row) {
    setEditingMargin(row.medicineId);
    setMarginDraft(row.marginPercent);
  }

  async function saveMargin(medicineId) {
    setError("");
    try {
      await updateMargin(selectedClinicId, medicineId, Number(marginDraft));
      setEditingMargin(null);
      loadClinicStock(selectedClinicId);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update the margin.");
    }
  }

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Stock, dispatch &amp; pricing</h2>
      </div>
      <p className="text-muted" style={{ marginTop: -8, marginBottom: 20 }}>
        Send medicine stock to a branch and set the chargeable margin here. Only HQ dispatches stock and sets
        pricing - branches can view stock and sell it, but can't set their own margin or see cost price.
      </p>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <form className="stock-form-inline card" onSubmit={handleDispatch}>
        <div className="field">
          <label>Branch</label>
          <select required value={form.clinicId} onChange={updateField("clinicId")}>
            <option value="">Select...</option>
            {clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Medicine</label>
          <select required value={form.medicineId} onChange={updateField("medicineId")}>
            <option value="">Select...</option>
            {medicines.map((m) => (
              <option key={m.id} value={m.id}>{m.name}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Batch #</label>
          <input value={form.batchNumber} onChange={updateField("batchNumber")} />
        </div>
        <div className="field">
          <label>Cost price (₹ per {selectedMedicine?.unit || "unit"})</label>
          <input required type="number" min="0" step="0.01" value={form.costPrice} onChange={updateField("costPrice")} />
        </div>
        <div className="field">
          <label>MRP (₹ per {selectedMedicine?.unit || "unit"})</label>
          <input required type="number" min="0" step="0.01" value={form.mrp} onChange={updateField("mrp")} />
        </div>
        <div className="field">
          <label>
            {selectedMedicine?.packSize
              ? `Quantity (in ${selectedMedicine.packLabel}s of ${selectedMedicine.packSize})`
              : `Quantity (in ${selectedMedicine?.unit || "units"})`}
          </label>
          <input required type="number" min="1" value={form.quantity} onChange={updateField("quantity")} />
        </div>
        <div className="field">
          <label>Expiry date</label>
          <input type="date" value={form.expiryDate} onChange={updateField("expiryDate")} />
        </div>
        <button className="btn btn-primary" disabled={saving} type="submit">
          {saving ? "Dispatching..." : "Dispatch stock"}
        </button>
      </form>

      <div className="section-header">
        <h3>Branch stock &amp; pricing</h3>
      </div>
      <div className="field" style={{ maxWidth: 320, marginBottom: 16 }}>
        <select
          value={selectedClinicId}
          onChange={(e) => {
            setSelectedClinicId(e.target.value);
            loadClinicStock(e.target.value);
          }}
        >
          <option value="">Select a branch...</option>
          {clinics.map((c) => (
            <option key={c.id} value={c.id}>{c.clinicName}</option>
          ))}
        </select>
      </div>
      {loadingStock ? (
        <p className="empty-state">Loading...</p>
      ) : selectedClinicId && clinicStock.length === 0 ? (
        <p className="empty-state">This branch has no stock dispatched yet.</p>
      ) : clinicStock.length > 0 ? (
        <div className="card" style={{ overflowX: "auto", marginBottom: 24 }}>
          <table className="data-table">
            <thead>
              <tr><th>Medicine</th><th>On hand</th><th>Cost</th><th>Margin %</th><th>Selling price</th><th>Nearest expiry</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {clinicStock.map((row) => (
                <tr key={row.medicineId} className={row.lowStock ? "row-lowstock" : ""}>
                  <td>{row.medicineName}</td>
                  <td>{row.quantityOnHand} {row.unit}(s) {row.lowStock && <span className="badge badge-danger chip-sm">Low</span>}</td>
                  <td>{row.currentCostPrice != null ? `₹${row.currentCostPrice}` : "-"}</td>
                  <td>
                    {editingMargin === row.medicineId ? (
                      <input
                        type="number" min="0" max="500" step="0.5" style={{ width: 80 }}
                        value={marginDraft}
                        onChange={(e) => setMarginDraft(e.target.value)}
                      />
                    ) : (
                      `${row.marginPercent}%`
                    )}
                  </td>
                  <td>{row.currentSellingPrice != null ? `₹${row.currentSellingPrice}` : "-"}</td>
                  <td>{row.nearestExpiryDate || "-"}</td>
                  <td className="actions">
                    {editingMargin === row.medicineId ? (
                      <>
                        <button className="btn btn-primary btn-sm" onClick={() => saveMargin(row.medicineId)}>Save</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => setEditingMargin(null)}>Cancel</button>
                      </>
                    ) : (
                      <button className="btn btn-secondary btn-sm" onClick={() => startEditMargin(row)}>Edit margin</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      <div className="section-header">
        <h3>Reconciliation - dispatched vs. sold vs. remaining</h3>
      </div>
      <p className="text-muted" style={{ marginTop: -8, marginBottom: 16 }}>
        "System remaining" is what the app calculates should be left. Compare it against each branch's
        physical shelf count - a gap there is a real signal worth investigating (miscounting, spoilage, or theft).
      </p>
      {loadingReconciliation ? (
        <p className="empty-state">Loading...</p>
      ) : reconciliation.length === 0 ? (
        <p className="empty-state">No stock dispatched yet - use the form above to get started.</p>
      ) : (
        <div className="card" style={{ overflowX: "auto" }}>
          <table className="data-table">
            <thead>
              <tr><th>Branch</th><th>Medicine</th><th>Dispatched</th><th>Sold</th><th>System remaining</th></tr>
            </thead>
            <tbody>
              {reconciliation.map((r, i) => (
                <tr key={i}>
                  <td>{r.clinicName}</td>
                  <td>{r.medicineName}</td>
                  <td>{r.totalDispatched} {r.unit}(s)</td>
                  <td>{r.totalSold} {r.unit}(s)</td>
                  <td>{r.systemRemaining} {r.unit}(s)</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardLayout>
  );
}
