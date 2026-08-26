import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  getMyClinics,
  getAllAssociations,
  inviteDoctor,
  approveJoinRequest,
  rejectJoinRequest,
  removeDoctorFromClinic,
  getClinicAvailability,
  activateClinicAvailability,
  deactivateClinicAvailability,
} from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

function statusBadgeClass(status) {
  if (status === "APPROVED") return "badge-success";
  if (status === "PENDING") return "badge-warning";
  if (status === "REJECTED") return "badge-danger";
  return "badge-muted";
}

function labelDay(day) {
  return day[0] + day.slice(1).toLowerCase();
}

export default function ClinicAdminDoctors() {
  const [clinics, setClinics] = useState([]);
  const [associations, setAssociations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busyId, setBusyId] = useState(null);

  const [inviteClinicId, setInviteClinicId] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviting, setInviting] = useState(false);

  const [availClinicId, setAvailClinicId] = useState("");
  const [availability, setAvailability] = useState([]);
  const [availLoading, setAvailLoading] = useState(false);
  const [togglingId, setTogglingId] = useState(null);

  function load() {
    setLoading(true);
    setError("");
    Promise.all([getMyClinics(), getAllAssociations()])
      .then(([c, a]) => {
        setClinics(c);
        setAssociations(a);
        setInviteClinicId((id) => id || (c[0] ? c[0].id : ""));
        setAvailClinicId((id) => id || (c[0] ? c[0].id : ""));
      })
      .catch(() => setError("Couldn't load your doctors."))
      .finally(() => setLoading(false));
  }

  function loadAvailability(clinicId) {
    if (!clinicId) return;
    setAvailLoading(true);
    getClinicAvailability(clinicId)
      .then(setAvailability)
      .catch(() => setError("Couldn't load doctors' hours for this clinic."))
      .finally(() => setAvailLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (availClinicId) loadAvailability(availClinicId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [availClinicId]);

  async function handleToggleAvailability(a) {
    setTogglingId(a.id);
    setError("");
    setMessage("");
    try {
      if (a.active) {
        await deactivateClinicAvailability(availClinicId, a.id);
        setMessage(`Turned off ${a.doctorName}'s ${labelDay(a.dayOfWeek)} ${a.startTime}-${a.endTime} hours.`);
      } else {
        await activateClinicAvailability(availClinicId, a.id);
        setMessage(`Turned back on ${a.doctorName}'s ${labelDay(a.dayOfWeek)} ${a.startTime}-${a.endTime} hours.`);
      }
      loadAvailability(availClinicId);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that availability window.");
    } finally {
      setTogglingId(null);
    }
  }

  async function submitInvite(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    setInviting(true);
    try {
      await inviteDoctor(Number(inviteClinicId), inviteEmail);
      setMessage("Invite sent. The doctor needs to accept it before they're associated with your clinic.");
      setInviteEmail("");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't send that invite. Double-check the doctor's email.");
    } finally {
      setInviting(false);
    }
  }

  async function handleApprove(id) {
    setBusyId(id);
    setError("");
    setMessage("");
    try {
      await approveJoinRequest(id);
      setMessage("Request approved.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't approve that request.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(id) {
    setBusyId(id);
    setError("");
    setMessage("");
    try {
      await rejectJoinRequest(id);
      setMessage("Request declined.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't decline that request.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleRemove(id) {
    setBusyId(id);
    setError("");
    setMessage("");
    try {
      await removeDoctorFromClinic(id);
      setMessage("Doctor removed from your clinic.");
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't remove that doctor.");
    } finally {
      setBusyId(null);
    }
  }

  const pendingFromDoctors = associations.filter((a) => a.status === "PENDING" && a.initiatedBy === "DOCTOR");

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Doctors</h2>
      </div>

      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>Invite a doctor</h3>
        {clinics.length === 0 ? (
          <p>Add a clinic first, then you can invite doctors to it.</p>
        ) : (
          <form onSubmit={submitInvite} className="form-row" style={{ alignItems: "flex-end" }}>
            <div className="field">
              <label htmlFor="invite-clinic">Clinic</label>
              <select id="invite-clinic" value={inviteClinicId} onChange={(e) => setInviteClinicId(e.target.value)}>
                {clinics.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.clinicName}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label htmlFor="invite-email">Doctor's account email</label>
              <input
                id="invite-email"
                type="email"
                required
                placeholder="doctor@example.com"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
              />
            </div>
            <button className="btn btn-primary" disabled={inviting} type="submit">
              {inviting ? "Sending..." : "Send invite"}
            </button>
          </form>
        )}
        <p style={{ fontSize: "0.85rem", marginTop: 10, color: "var(--ink-soft)" }}>
          The doctor must already have a doctor account with this email. They'll see the invite on their dashboard
          and need to accept it before they're listed at your clinic.
        </p>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : (
        <>
          {pendingFromDoctors.length > 0 && (
            <div className="card" style={{ marginBottom: 24 }}>
              <h3>Doctors requesting to join</h3>
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Doctor</th>
                      <th>Clinic</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pendingFromDoctors.map((a) => (
                      <tr key={a.id}>
                        <td>
                          {a.doctorName}
                          <br />
                          <span style={{ color: "var(--ink-soft)", fontSize: "0.85rem" }}>
                            {a.doctorQualification || "—"}
                          </span>
                        </td>
                        <td>{a.clinicName}</td>
                        <td className="actions">
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={busyId === a.id}
                            onClick={() => handleApprove(a.id)}
                          >
                            Approve
                          </button>
                          <button
                            className="btn btn-secondary btn-sm"
                            disabled={busyId === a.id}
                            onClick={() => handleReject(a.id)}
                          >
                            Decline
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="card">
            <h3>All doctors</h3>
            {associations.length === 0 ? (
              <div className="empty-state">No invites or requests yet.</div>
            ) : (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Doctor</th>
                      <th>Clinic</th>
                      <th>Initiated by</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {associations.map((a) => (
                      <tr key={a.id}>
                        <td>{a.doctorName}</td>
                        <td>{a.clinicName}</td>
                        <td>{a.initiatedBy === "CLINIC" ? "You" : "Doctor"}</td>
                        <td>
                          <span className={`badge ${statusBadgeClass(a.status)}`}>{a.status}</span>
                        </td>
                        <td className="actions">
                          {a.status === "APPROVED" && (
                            <button
                              className="btn btn-secondary btn-sm"
                              disabled={busyId === a.id}
                              onClick={() => handleRemove(a.id)}
                            >
                              Remove
                            </button>
                          )}
                          {a.status === "PENDING" && a.initiatedBy === "CLINIC" && (
                            <span style={{ color: "var(--ink-soft)" }}>Awaiting doctor's response</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="card" style={{ marginTop: 24 }}>
            <h3>Doctors' hours at your clinic</h3>
            <p style={{ fontSize: "0.85rem", color: "var(--ink-soft)", marginBottom: 12 }}>
              Doctors set their own weekly hours, but you have the final say on what's actually bookable at your
              clinic. Turn a window off to pull it from booking without removing the doctor entirely.
            </p>
            {clinics.length === 0 ? (
              <p>Add a clinic first to review doctors' hours.</p>
            ) : (
              <>
                <div className="field" style={{ maxWidth: 320, marginBottom: 16 }}>
                  <label htmlFor="avail-clinic">Clinic</label>
                  <select id="avail-clinic" value={availClinicId} onChange={(e) => setAvailClinicId(e.target.value)}>
                    {clinics.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.clinicName}
                      </option>
                    ))}
                  </select>
                </div>

                {availLoading ? (
                  <p>Loading...</p>
                ) : availability.length === 0 ? (
                  <div className="empty-state">No doctor has set hours at this clinic yet.</div>
                ) : (
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Doctor</th>
                          <th>Day</th>
                          <th>Time</th>
                          <th>Slot length</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {availability.map((a) => (
                          <tr key={a.id}>
                            <td>{a.doctorName}</td>
                            <td>{labelDay(a.dayOfWeek)}</td>
                            <td>
                              {a.startTime}–{a.endTime}
                            </td>
                            <td>{a.slotDurationMinutes} min</td>
                            <td>
                              <span className={`badge ${a.active ? "badge-success" : "badge-muted"}`}>
                                {a.active ? "Active" : "Turned off"}
                              </span>
                            </td>
                            <td className="actions">
                              <button
                                className={`btn btn-sm ${a.active ? "btn-secondary" : "btn-primary"}`}
                                disabled={togglingId === a.id}
                                onClick={() => handleToggleAvailability(a)}
                              >
                                {togglingId === a.id ? "Updating..." : a.active ? "Turn off" : "Turn on"}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
