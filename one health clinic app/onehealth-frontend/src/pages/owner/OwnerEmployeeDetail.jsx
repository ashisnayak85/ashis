import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import DashboardLayout from "../../components/DashboardLayout";
import { getEmployeeProfile, updateEmployeeProfile, addSalaryRecord } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

const emptyProfileForm = { gender: "", dob: "", dateOfJoining: "", permanentAddress: "", currentAddress: "" };
const emptySalaryForm = { amount: "", effectiveFrom: new Date().toISOString().slice(0, 10) };

/**
 * Full HR view for one staff member: profile fields + complete salary history
 * (never overwritten - every raise/revision is its own row, appended). Owner-only.
 */
export default function OwnerEmployeeDetail() {
  const { userId } = useParams();
  const [profile, setProfile] = useState(null);
  const [profileForm, setProfileForm] = useState(emptyProfileForm);
  const [salaryForm, setSalaryForm] = useState(emptySalaryForm);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  function load() {
    setLoading(true);
    getEmployeeProfile(userId)
      .then((p) => {
        setProfile(p);
        setProfileForm({
          gender: p.gender || "",
          dob: p.dob || "",
          dateOfJoining: p.dateOfJoining || "",
          permanentAddress: p.permanentAddress || "",
          currentAddress: p.currentAddress || "",
        });
      })
      .catch(() => setError("Couldn't load this staff member's profile."))
      .finally(() => setLoading(false));
  }

  useEffect(load, [userId]);

  function updateProfileField(field) {
    return (e) => setProfileForm((f) => ({ ...f, [field]: e.target.value }));
  }
  function updateSalaryField(field) {
    return (e) => setSalaryForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    setError("");
    try {
      await updateEmployeeProfile(userId, {
        ...profileForm,
        gender: profileForm.gender || null,
        dob: profileForm.dob || null,
        dateOfJoining: profileForm.dateOfJoining || null,
      });
      setMessage("Profile saved.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't save profile.");
    }
  }

  async function handleAddSalary(e) {
    e.preventDefault();
    setError("");
    try {
      await addSalaryRecord(userId, { amount: Number(salaryForm.amount), effectiveFrom: salaryForm.effectiveFrom });
      setSalaryForm({ amount: "", effectiveFrom: new Date().toISOString().slice(0, 10) });
      setMessage("Salary revision recorded.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't record that salary revision.");
    }
  }

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>{loading ? "Staff member" : profile?.name}</h2>
      </div>
      <p style={{ marginTop: -12 }}><Link to="/owner/employees">← Back to Staff / HR</Link></p>
      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      {loading || !profile ? (
        <p>Loading...</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-value">{profile.role === "CLINIC_ADMIN" ? "Clinic authority" : "Doctor"}</div>
              <div className="stat-label">Role</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{profile.clinicNames?.join(", ") || "-"}</div>
              <div className="stat-label">Branch(es)</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">
                {profile.currentSalary != null ? `₹${Number(profile.currentSalary).toLocaleString()}` : "Not set"}
              </div>
              <div className="stat-label">Current salary{profile.currentSalaryEffectiveFrom ? ` (from ${profile.currentSalaryEffectiveFrom})` : ""}</div>
            </div>
          </div>

          <div className="form-row">
            <form className="form-card wide" onSubmit={handleSaveProfile}>
              <h3>HR profile</h3>
              <div className="field">
                <label>Gender</label>
                <select value={profileForm.gender} onChange={updateProfileField("gender")}>
                  <option value="">Not set</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="field">
                <label>Date of birth</label>
                <input type="date" value={profileForm.dob} onChange={updateProfileField("dob")} />
              </div>
              <div className="field">
                <label>Date of joining</label>
                <input type="date" value={profileForm.dateOfJoining} onChange={updateProfileField("dateOfJoining")} />
              </div>
              <div className="field">
                <label>Permanent address</label>
                <input value={profileForm.permanentAddress} onChange={updateProfileField("permanentAddress")} />
              </div>
              <div className="field">
                <label>Current address</label>
                <input value={profileForm.currentAddress} onChange={updateProfileField("currentAddress")} />
              </div>
              <button className="btn btn-primary" type="submit">Save profile</button>
            </form>

            <form className="form-card wide" onSubmit={handleAddSalary}>
              <h3>Record a salary revision</h3>
              <p style={{ fontSize: "0.85rem", color: "var(--ink-soft)" }}>
                This adds a new entry to the salary history below - it never overwrites
                a previous amount, so past pay is always preserved.
              </p>
              <div className="field">
                <label>Amount</label>
                <input type="number" min="0" required value={salaryForm.amount} onChange={updateSalaryField("amount")} />
              </div>
              <div className="field">
                <label>Effective from</label>
                <input type="date" required value={salaryForm.effectiveFrom} onChange={updateSalaryField("effectiveFrom")} />
              </div>
              <button className="btn btn-primary" type="submit">Add revision</button>
            </form>
          </div>

          <div className="section-header">
            <h3>Salary history</h3>
          </div>
          {profile.salaryHistory.length === 0 ? (
            <p className="empty-state">No salary revisions recorded yet.</p>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead><tr><th>Amount</th><th>Effective from</th><th>Recorded on</th></tr></thead>
                <tbody>
                  {profile.salaryHistory.map((r) => (
                    <tr key={r.id}>
                      <td>₹{Number(r.amount).toLocaleString()}</td>
                      <td>{r.effectiveFrom}</td>
                      <td>{new Date(r.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </DashboardLayout>
  );
}
