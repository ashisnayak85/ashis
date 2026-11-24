import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  getDoctors, registerDoctor, setDoctorStatus, getClinics, assignDoctor, unassignDoctor,
  getSpecializations, updateDoctorSpecializations,
} from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

const emptyDoctor = {
  name: "", email: "", password: "", qualification: "",
  experienceYears: "", consultationFee: "", clinicIds: [], specializationIds: [],
};

export default function OwnerDoctors() {
  const [doctors, setDoctors] = useState([]);
  const [clinics, setClinics] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [form, setForm] = useState(emptyDoctor);

  function load() {
    setLoading(true);
    Promise.all([getDoctors(), getClinics(), getSpecializations()])
      .then(([d, c, s]) => { setDoctors(d); setClinics(c); setSpecializations(s); })
      .catch(() => setError("Couldn't load doctors."))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  function toggleClinic(clinicId) {
    setForm((f) => {
      const has = f.clinicIds.includes(clinicId);
      return { ...f, clinicIds: has ? f.clinicIds.filter((id) => id !== clinicId) : [...f.clinicIds, clinicId] };
    });
  }

  function toggleSpecialization(specId) {
    setForm((f) => {
      const has = f.specializationIds.includes(specId);
      return { ...f, specializationIds: has ? f.specializationIds.filter((id) => id !== specId) : [...f.specializationIds, specId] };
    });
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError("");
    try {
      await registerDoctor({
        ...form,
        experienceYears: form.experienceYears ? Number(form.experienceYears) : null,
        consultationFee: form.consultationFee ? Number(form.consultationFee) : null,
      });
      setForm(emptyDoctor);
      setMessage("Doctor added.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add doctor.");
    }
  }

  async function handleToggleStatus(doctor) {
    try {
      await setDoctorStatus(doctor.id, !doctor.active);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update doctor status.");
    }
  }

  async function handleAssign(doctorId, clinicId) {
    if (!clinicId) return;
    try {
      await assignDoctor(doctorId, clinicId);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't assign doctor - check for a schedule overlap.");
    }
  }

  async function handleUnassign(doctorId, clinicId) {
    try {
      await unassignDoctor(doctorId, clinicId);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't unassign doctor.");
    }
  }

  async function handleToggleDoctorSpecialization(doctor, specId) {
    const current = doctor.specializations.map((s) => s.id);
    const next = current.includes(specId) ? current.filter((id) => id !== specId) : [...current, specId];
    try {
      await updateDoctorSpecializations(doctor.id, next);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update this doctor's specializations.");
    }
  }

  const activeSpecializations = specializations.filter((s) => s.active);

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Doctors</h2>
      </div>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      {activeSpecializations.length === 0 && (
        <div className="form-error">
          No specializations set up yet - add some on the{" "}
          <a href="/owner/specializations">Specializations</a> page first so you can tag doctors with them.
        </div>
      )}

      <div className="form-card wide">
        <h3>Add a new doctor</h3>
        <form onSubmit={handleCreate}>
          <div className="field">
            <label>Name</label>
            <input required value={form.name} onChange={update("name")} />
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" required value={form.email} onChange={update("email")} />
          </div>
          <div className="field">
            <label>Temporary password</label>
            <input type="password" required value={form.password} onChange={update("password")} />
          </div>
          <div className="field">
            <label>Specializations (select all that apply)</label>
            <div className="chip-group">
              {activeSpecializations.map((s) => (
                <button
                  type="button"
                  key={s.id}
                  className={`chip ${form.specializationIds.includes(s.id) ? "chip-selected" : ""}`}
                  onClick={() => toggleSpecialization(s.id)}
                >
                  {s.name}
                </button>
              ))}
            </div>
          </div>
          <div className="field">
            <label>Qualification</label>
            <input value={form.qualification} onChange={update("qualification")} />
          </div>
          <div className="field">
            <label>Experience (years)</label>
            <input type="number" min="0" value={form.experienceYears} onChange={update("experienceYears")} />
          </div>
          <div className="field">
            <label>Consultation fee</label>
            <input type="number" min="0" value={form.consultationFee} onChange={update("consultationFee")} />
          </div>
          <div className="field">
            <label>Assign to branches now (optional)</label>
            <div className="chip-group">
              {clinics.map((c) => (
                <button
                  type="button"
                  key={c.id}
                  className={`chip ${form.clinicIds.includes(c.id) ? "chip-selected" : ""}`}
                  onClick={() => toggleClinic(c.id)}
                >
                  {c.clinicName}
                </button>
              ))}
            </div>
          </div>
          <button className="btn btn-primary" type="submit">Add doctor</button>
        </form>
      </div>

      <div className="section-header">
        <h3>All doctors</h3>
      </div>
      {loading ? (
        <p>Loading...</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Name</th><th>Specializations</th><th>Assigned branches</th><th>Assign to</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {doctors.map((d) => (
                <tr key={d.id}>
                  <td>{d.name}</td>
                  <td>
                    <div className="chip-group">
                      {activeSpecializations.map((s) => {
                        const has = d.specializations.some((ds) => ds.id === s.id);
                        return (
                          <button
                            key={s.id}
                            type="button"
                            className={`chip chip-sm ${has ? "chip-selected" : ""}`}
                            onClick={() => handleToggleDoctorSpecialization(d, s.id)}
                          >
                            {s.name}
                          </button>
                        );
                      })}
                    </div>
                  </td>
                  <td>
                    {d.assignedClinics.length === 0 ? (
                      <span className="badge badge-warning">No branches</span>
                    ) : (
                      d.assignedClinics.map((c) => (
                        <span key={c.id} className="badge badge-success" style={{ marginRight: 6, marginBottom: 4, display: "inline-block" }}>
                          {c.clinicName}{" "}
                          <button
                            onClick={() => handleUnassign(d.id, c.id)}
                            style={{ border: "none", background: "none", cursor: "pointer", fontWeight: 700 }}
                            title="Unassign"
                          >
                            ×
                          </button>
                        </span>
                      ))
                    )}
                  </td>
                  <td>
                    <select defaultValue="" onChange={(e) => { handleAssign(d.id, e.target.value); e.target.value = ""; }}>
                      <option value="">+ Assign branch</option>
                      {clinics
                        .filter((c) => !d.assignedClinics.some((ac) => ac.id === c.id))
                        .map((c) => (
                          <option key={c.id} value={c.id}>{c.clinicName}</option>
                        ))}
                    </select>
                  </td>
                  <td>
                    <span className={`badge ${d.active ? "badge-success" : "badge-muted"}`}>
                      {d.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => handleToggleStatus(d)}>
                      {d.active ? "Deactivate" : "Activate"}
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
