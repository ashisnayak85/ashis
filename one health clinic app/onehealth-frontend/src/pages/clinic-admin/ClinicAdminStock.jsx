import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyStock, updateReorderThreshold } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

/**
 * This branch's current stock - selling price only. Cost price and margin
 * are deliberately not shown here or returned by the API at all: pricing is
 * set centrally by the owner (see OwnerStockDispatch), so a branch can't see
 * or reconstruct its own cost/margin. The only thing this screen lets a
 * clinic admin adjust is the reorder threshold (when to flag "running low"),
 * which is operational, not a pricing decision.
 */
export default function ClinicAdminStock() {
  const [stock, setStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(null);
  const [draft, setDraft] = useState("");

  function load() {
    setLoading(true);
    getMyStock()
      .then(setStock)
      .catch(() => setError("Couldn't load stock."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function startEdit(row) {
    setEditing(row.medicineId);
    setDraft(row.reorderThreshold);
  }

  async function save(medicineId) {
    setError("");
    try {
      await updateReorderThreshold(medicineId, Number(draft));
      setEditing(null);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update the reorder threshold.");
    }
  }

  return (
    <DashboardLayout title="Clinic Admin Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Medicine stock</h2>
      </div>
      <p className="text-muted" style={{ marginTop: -8, marginBottom: 20 }}>
        Selling prices are set by the owner and shown here as-is - this branch can't change pricing. You can
        set when a medicine should be flagged as running low.
      </p>
      {error && <div className="form-error">{error}</div>}
      {loading ? (
        <p className="empty-state">Loading...</p>
      ) : stock.length === 0 ? (
        <p className="empty-state">No stock has been dispatched to this branch yet.</p>
      ) : (
        <div className="card" style={{ overflowX: "auto" }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Medicine</th>
                <th>On hand</th>
                <th>Selling price</th>
                <th>Reorder threshold</th>
                <th>Nearest expiry</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {stock.map((row) => (
                <tr key={row.medicineId} className={row.lowStock ? "row-lowstock" : ""}>
                  <td>
                    {row.medicineName}
                    {row.scheduledDrug && <span className="badge badge-warning chip-sm" style={{ marginLeft: 6 }}>Scheduled</span>}
                  </td>
                  <td>
                    {row.quantityOnHand} {row.unit}(s)
                    {row.lowStock && <span className="badge badge-danger chip-sm" style={{ marginLeft: 6 }}>Low</span>}
                  </td>
                  <td>{row.sellingPrice != null ? `₹${row.sellingPrice}` : "-"}</td>
                  <td>
                    {editing === row.medicineId ? (
                      <input
                        type="number" min="0" style={{ width: 80 }}
                        value={draft}
                        onChange={(e) => setDraft(e.target.value)}
                      />
                    ) : (
                      row.reorderThreshold
                    )}
                  </td>
                  <td>{row.nearestExpiryDate || "-"}</td>
                  <td className="actions">
                    {editing === row.medicineId ? (
                      <>
                        <button className="btn btn-primary btn-sm" onClick={() => save(row.medicineId)}>Save</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => setEditing(null)}>Cancel</button>
                      </>
                    ) : (
                      <button className="btn btn-secondary btn-sm" onClick={() => startEdit(row)}>Edit threshold</button>
                    )}
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
