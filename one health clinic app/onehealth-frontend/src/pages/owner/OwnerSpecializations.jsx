import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  getSpecializations, createSpecialization, renameSpecialization,
  setSpecializationStatus, deleteSpecialization,
} from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

/** Owner-managed master list, referenced by doctors via multi-select - see backend Specialization entity javadoc for why this replaced free text. */
export default function OwnerSpecializations() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [newName, setNewName] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editingName, setEditingName] = useState("");

  function load() {
    setLoading(true);
    getSpecializations().then(setItems).catch(() => setError("Couldn't load specializations.")).finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCreate(e) {
    e.preventDefault();
    setError("");
    try {
      await createSpecialization(newName.trim());
      setNewName("");
      setMessage("Specialization added.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add that specialization.");
    }
  }

  function startEdit(item) {
    setEditingId(item.id);
    setEditingName(item.name);
  }

  async function saveEdit(id) {
    setError("");
    try {
      await renameSpecialization(id, editingName.trim());
      setEditingId(null);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't rename that specialization.");
    }
  }

  async function handleToggle(item) {
    try {
      await setSpecializationStatus(item.id, !item.active);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update status.");
    }
  }

  async function handleDelete(item) {
    if (!window.confirm(`Delete "${item.name}"? This only works if no doctor currently has it.`)) return;
    setError("");
    try {
      await deleteSpecialization(item.id);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't delete that specialization - it's probably in use. Try deactivating instead.");
    }
  }

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Specializations</h2>
      </div>
      <p style={{ color: "var(--ink-soft)", marginTop: -8 }}>
        This is the master list doctors are selected from (multi-select, on the Doctors page) -
        keeping it here instead of free text avoids duplicate/inconsistent entries like
        "Cardiologist" vs "cardiologist".
      </p>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <form className="form-card wide" onSubmit={handleCreate}>
        <h3>Add a specialization</h3>
        <div className="field">
          <label>Name</label>
          <input required value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="e.g. Cardiology" />
        </div>
        <button className="btn btn-primary" type="submit">Add</button>
      </form>

      {loading ? (
        <p>Loading...</p>
      ) : items.length === 0 ? (
        <p className="empty-state">No specializations yet - add your first one above.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Name</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>
                    {editingId === item.id ? (
                      <input value={editingName} onChange={(e) => setEditingName(e.target.value)} />
                    ) : (
                      item.name
                    )}
                  </td>
                  <td>
                    <span className={`badge ${item.active ? "badge-success" : "badge-muted"}`}>
                      {item.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="actions">
                    {editingId === item.id ? (
                      <>
                        <button className="btn btn-secondary btn-sm" onClick={() => saveEdit(item.id)}>Save</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => setEditingId(null)}>Cancel</button>
                      </>
                    ) : (
                      <>
                        <button className="btn btn-secondary btn-sm" onClick={() => startEdit(item)}>Rename</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => handleToggle(item)}>
                          {item.active ? "Deactivate" : "Activate"}
                        </button>
                        <button className="btn btn-secondary btn-sm" onClick={() => handleDelete(item)}>Delete</button>
                      </>
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
