import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMedicines, createMedicine, setMedicineStatus } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

const EMPTY_FORM = {
  name: "",
  genericName: "",
  manufacturer: "",
  unit: "tablet",
  category: "",
  scheduledDrug: false,
  packSize: "",
  packLabel: "strip",
};

/**
 * Org-wide medicine catalog - the master list every branch picks from when
 * restocking (see OwnerStockDispatch for dispatch/pricing, ClinicAdminStock
 * for the per-branch view). No price is set here at all - cost/MRP/margin
 * are all decided later, per dispatch/branch (see OwnerStockDispatch).
 *
 * "Scheduled" flags a DPCO/NLEM price-controlled medicine, where a branch's
 * margin is legally capped at 16% - see MedicineService for where that cap
 * is enforced. "Pack size" is optional: set it when this medicine is
 * physically received/dispatched in bulk (e.g. a strip of 10 tablets) - it's
 * only a dispatch-time convenience, stock is always tracked and sold down to
 * a single loose unit regardless.
 */
export default function OwnerMedicines() {
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [showInactive, setShowInactive] = useState(false);
  const [usePack, setUsePack] = useState(false);

  function load() {
    setLoading(true);
    getMedicines()
      .then(setMedicines)
      .catch(() => setError("Couldn't load the medicine catalog."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function update(field) {
    return (e) => {
      const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
      setForm((f) => ({ ...f, [field]: value }));
    };
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      await createMedicine({
        ...form,
        packSize: usePack && form.packSize ? Number(form.packSize) : null,
        packLabel: usePack ? form.packLabel : null,
      });
      setForm(EMPTY_FORM);
      setUsePack(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add that medicine.");
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(m) {
    try {
      await setMedicineStatus(m.id, !m.active);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that medicine.");
    }
  }

  const visible = medicines.filter((m) => showInactive || m.active);

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Medicine catalog</h2>
      </div>
      {error && <div className="form-error">{error}</div>}

      <form className="form-card wide" onSubmit={handleCreate} style={{ marginBottom: 24 }}>
        <h3>Add a medicine</h3>
        <p className="field-hint">No pricing here - cost, MRP and margin are all set later at dispatch time.</p>
        <div className="field-row">
          <div className="field">
            <label>Name</label>
            <input required value={form.name} onChange={update("name")} placeholder="e.g. Paracetamol 500mg" />
          </div>
          <div className="field">
            <label>Generic name</label>
            <input value={form.genericName} onChange={update("genericName")} />
          </div>
        </div>
        <div className="field-row">
          <div className="field">
            <label>Manufacturer</label>
            <input value={form.manufacturer} onChange={update("manufacturer")} />
          </div>
          <div className="field">
            <label>Base unit (how it's actually dispensed/sold)</label>
            <select value={form.unit} onChange={update("unit")}>
              <option value="tablet">tablet</option>
              <option value="capsule">capsule</option>
              <option value="ml">ml</option>
              <option value="bottle">bottle</option>
              <option value="vial">vial</option>
              <option value="tube">tube</option>
              <option value="sachet">sachet</option>
            </select>
          </div>
        </div>
        <div className="field-row">
          <div className="field">
            <label>Category</label>
            <input value={form.category} onChange={update("category")} placeholder="e.g. Analgesic" />
          </div>
        </div>
        <div className="field" style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <input type="checkbox" id="usePack" checked={usePack} onChange={(e) => setUsePack(e.target.checked)} />
          <label htmlFor="usePack" style={{ margin: 0 }}>
            Received/dispatched in bulk packs (e.g. a strip of tablets)
          </label>
        </div>
        {usePack && (
          <div className="field-row">
            <div className="field">
              <label>Units per pack (e.g. 10 tablets per strip)</label>
              <input type="number" min="2" value={form.packSize} onChange={update("packSize")} />
            </div>
            <div className="field">
              <label>Pack label</label>
              <select value={form.packLabel} onChange={update("packLabel")}>
                <option value="strip">strip</option>
                <option value="box">box</option>
                <option value="carton">carton</option>
              </select>
            </div>
          </div>
        )}
        <p className="field-hint">
          Even when dispatched by the pack, stock is always tracked and sold down to a single loose {form.unit} -
          useful where a patient can only afford part of a strip.
        </p>
        <div className="field" style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <input type="checkbox" id="scheduledDrug" checked={form.scheduledDrug} onChange={update("scheduledDrug")} />
          <label htmlFor="scheduledDrug" style={{ margin: 0 }}>
            Price-controlled (scheduled / NLEM) medicine
          </label>
        </div>
        <p className="field-hint">
          Scheduled medicines fall under DPCO - branches can't be given a margin above the legal 16% cap on these.
        </p>
        <button className="btn btn-primary" disabled={saving} type="submit">
          {saving ? "Adding..." : "Add medicine"}
        </button>
      </form>

      <div className="section-header">
        <h3>All medicines</h3>
        <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.85rem" }}>
          <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />
          Show inactive
        </label>
      </div>

      {loading ? (
        <p className="empty-state">Loading...</p>
      ) : visible.length === 0 ? (
        <p className="empty-state">No medicines yet - add your first one above.</p>
      ) : (
        <div className="card" style={{ overflowX: "auto" }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Base unit</th>
                <th>Pack</th>
                <th>Category</th>
                <th>Pricing</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((m) => (
                <tr key={m.id}>
                  <td>
                    <strong>{m.name}</strong>
                    {m.genericName && <div className="text-muted" style={{ fontSize: "0.8rem" }}>{m.genericName}</div>}
                  </td>
                  <td>{m.unit}</td>
                  <td>{m.packSize ? `${m.packSize} ${m.unit}(s) / ${m.packLabel}` : "sold individually"}</td>
                  <td>{m.category || "-"}</td>
                  <td>
                    {m.scheduledDrug ? (
                      <span className="badge badge-warning">Scheduled - 16% cap</span>
                    ) : (
                      <span className="badge badge-muted">Free pricing</span>
                    )}
                  </td>
                  <td>
                    <span className={`badge ${m.active ? "badge-success" : "badge-muted"}`}>
                      {m.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => toggleStatus(m)}>
                      {m.active ? "Deactivate" : "Activate"}
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
