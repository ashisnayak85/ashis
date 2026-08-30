import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import ClinicLocationPicker from "../../components/ClinicLocationPicker";
import { getClinics, createClinic, setClinicStatus, registerClinicAdmin } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

const emptyClinic = { clinicName: "", address: "", city: "", pincode: "", phone: "", latitude: null, longitude: null };
const emptyAdmin = { clinicId: "", name: "", email: "", password: "", phone: "" };

export default function OwnerClinics() {
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [form, setForm] = useState(emptyClinic);
  const [adminForm, setAdminForm] = useState(emptyAdmin);

  function load() {
    setLoading(true);
    getClinics().then(setClinics).catch(() => setError("Couldn't load branches.")).finally(() => setLoading(false));
  }

  useEffect(load, []);

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }
  function updateAdmin(field) {
    return (e) => setAdminForm((f) => ({ ...f, [field]: e.target.value }));
  }
  function handleLocationChange(patch) {
    setForm((f) => ({ ...f, ...patch }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError("");
    if (form.latitude == null || form.longitude == null) {
      setError("Pin the branch's location on the map before saving - this is what powers accurate directions for patients.");
      return;
    }
    try {
      await createClinic(form);
      setForm(emptyClinic);
      setMessage("Branch created.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't create branch.");
    }
  }

  async function handleToggle(clinic) {
    try {
      await setClinicStatus(clinic.id, !clinic.active);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update branch status.");
    }
  }

  async function handleCreateAdmin(e) {
    e.preventDefault();
    setError("");
    try {
      await registerClinicAdmin(adminForm);
      setAdminForm(emptyAdmin);
      setMessage("Clinic authority account created.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't create the clinic authority account.");
    }
  }

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Branches</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <div className="form-row">
        <div className="form-card wide">
          <h3>Add a new branch</h3>
          <form onSubmit={handleCreate}>
            <div className="field">
              <label>Branch name</label>
              <input required value={form.clinicName} onChange={update("clinicName")} />
            </div>

            <ClinicLocationPicker
              latitude={form.latitude}
              longitude={form.longitude}
              onChange={handleLocationChange}
            />

            <div className="field" style={{ marginTop: 16 }}>
              <label>Address (edit if needed)</label>
              <input required value={form.address} onChange={update("address")} />
            </div>
            <div className="field">
              <label>City</label>
              <input value={form.city} onChange={update("city")} />
            </div>
            <div className="field">
              <label>Pincode</label>
              <input value={form.pincode} onChange={update("pincode")} />
            </div>
            <div className="field">
              <label>Phone</label>
              <input value={form.phone} onChange={update("phone")} />
            </div>
            <button className="btn btn-primary" type="submit">Add branch</button>
          </form>
        </div>

        <div className="form-card wide">
          <h3>Create a clinic authority login</h3>
          <p>The front-desk account for one branch - books walk-ins, manages that branch's schedule.</p>
          <form onSubmit={handleCreateAdmin}>
            <div className="field">
              <label>Branch</label>
              <select required value={adminForm.clinicId} onChange={updateAdmin("clinicId")}>
                <option value="">Select a branch</option>
                {clinics.map((c) => (
                  <option key={c.id} value={c.id}>{c.clinicName}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Name</label>
              <input required value={adminForm.name} onChange={updateAdmin("name")} />
            </div>
            <div className="field">
              <label>Email</label>
              <input type="email" required value={adminForm.email} onChange={updateAdmin("email")} />
            </div>
            <div className="field">
              <label>Temporary password</label>
              <input type="password" required value={adminForm.password} onChange={updateAdmin("password")} />
            </div>
            <div className="field">
              <label>Phone</label>
              <input value={adminForm.phone} onChange={updateAdmin("phone")} />
            </div>
            <button className="btn btn-primary" type="submit">Create login</button>
          </form>
        </div>
      </div>

      <div className="section-header">
        <h3>All branches</h3>
      </div>
      {loading ? (
        <p>Loading...</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Name</th><th>City</th><th>Clinic authority</th><th>Doctors</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {clinics.map((c) => (
                <tr key={c.id}>
                  <td>{c.clinicName}</td>
                  <td>{c.city}</td>
                  <td>{c.clinicAdminName || <span className="badge badge-warning">Not set up</span>}</td>
                  <td>{c.doctorCount}</td>
                  <td>
                    <span className={`badge ${c.active ? "badge-success" : "badge-muted"}`}>
                      {c.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => handleToggle(c)}>
                      {c.active ? "Deactivate" : "Activate"}
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
