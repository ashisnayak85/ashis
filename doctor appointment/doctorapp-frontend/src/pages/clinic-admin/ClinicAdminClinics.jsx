import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import LocationPicker from "../../components/LocationPicker";
import { getMyClinics, createClinic } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

export default function ClinicAdminClinics() {
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const [pickerKey, setPickerKey] = useState(0);

  const [form, setForm] = useState({
    clinicName: "",
    address: "",
    latitude: "",
    longitude: "",
    city: "",
    pincode: "",
    phone: "",
  });

  function load() {
    setLoading(true);
    getMyClinics()
      .then(setClinics)
      .catch(() => setError("Couldn't load your clinics."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  function updateForm(field) {
    return (e) => setForm({ ...form, [field]: e.target.value });
  }

  function handleLocationPick(lat, lng) {
    setForm((f) => ({ ...f, latitude: String(lat), longitude: String(lng) }));
  }

  async function submit(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    if (!form.latitude || !form.longitude) {
      setError("Pick the clinic's location on the map before submitting.");
      return;
    }
    setSaving(true);
    try {
      await createClinic({
        ...form,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
      });
      setMessage("Clinic added. An admin will verify it before it accepts doctors or appears in patient search.");
      setForm({ clinicName: "", address: "", latitude: "", longitude: "", city: "", pincode: "", phone: "" });
      setPickerKey((k) => k + 1); // remount the map picker so its pin/search box clear too
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add that clinic.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>My Clinics</h2>
      </div>

      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>Add a clinic</h3>
        <form onSubmit={submit}>
          <div className="form-row">
            <div className="field">
              <label htmlFor="c-name">Clinic name</label>
              <input id="c-name" required value={form.clinicName} onChange={updateForm("clinicName")} />
            </div>
            <div className="field">
              <label htmlFor="c-phone">Phone</label>
              <input id="c-phone" value={form.phone} onChange={updateForm("phone")} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="c-address">Address</label>
            <input id="c-address" required value={form.address} onChange={updateForm("address")} />
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="c-city">City</label>
              <input id="c-city" value={form.city} onChange={updateForm("city")} />
            </div>
            <div className="field">
              <label htmlFor="c-pincode">Pincode</label>
              <input id="c-pincode" value={form.pincode} onChange={updateForm("pincode")} />
            </div>
          </div>
          <div className="field">
            <label>Clinic location</label>
            <LocationPicker
              key={pickerKey}
              latitude={form.latitude}
              longitude={form.longitude}
              onChange={handleLocationPick}
            />
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="c-lat">Latitude</label>
              <input id="c-lat" type="number" step="any" readOnly value={form.latitude} />
            </div>
            <div className="field">
              <label htmlFor="c-lng">Longitude</label>
              <input id="c-lng" type="number" step="any" readOnly value={form.longitude} />
            </div>
          </div>
          <button className="btn btn-primary btn-block" disabled={saving} type="submit">
            {saving ? "Saving..." : "Add clinic"}
          </button>
        </form>
      </div>

      <div className="card">
        <h3>Your clinics</h3>
        {loading ? (
          <p>Loading...</p>
        ) : clinics.length === 0 ? (
          <div className="empty-state">No clinics yet. Add one above.</div>
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
                        {c.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
