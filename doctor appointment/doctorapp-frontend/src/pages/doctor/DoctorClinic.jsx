import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import {
  browseClinics,
  getMyClinicAssociations,
  requestJoinClinic,
  approveClinicInvite,
  rejectClinicInvite,
  leaveClinic,
  addAvailability,
  getMyAvailability,
  deleteAvailability,
} from "../../api/doctorDashboard";
import { DOCTOR_NAV_ITEMS } from "./navItems";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function labelDay(day) {
  return day[0] + day.slice(1).toLowerCase();
}

function statusBadgeClass(status) {
  if (status === "APPROVED") return "badge-success";
  if (status === "PENDING") return "badge-warning";
  if (status === "REJECTED") return "badge-danger";
  return "badge-muted";
}

export default function DoctorClinic() {
  const [associations, setAssociations] = useState([]);
  const [browseResults, setBrowseResults] = useState([]);
  const [cityFilter, setCityFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [browsing, setBrowsing] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busyId, setBusyId] = useState(null);

  const [availForm, setAvailForm] = useState({
    clinicId: "",
    dayOfWeek: "MONDAY",
    startTime: "09:00",
    endTime: "13:00",
    slotDurationMinutes: 15,
  });
  const [savingAvail, setSavingAvail] = useState(false);

  const [myAvailability, setMyAvailability] = useState([]);
  const [availLoading, setAvailLoading] = useState(true);
  const [deletingAvailId, setDeletingAvailId] = useState(null);

  function loadAvailability() {
    setAvailLoading(true);
    getMyAvailability()
      .then(setMyAvailability)
      .catch(() => setError("Couldn't load your weekly hours."))
      .finally(() => setAvailLoading(false));
  }

  const approvedClinics = associations
    .filter((a) => a.status === "APPROVED")
    .map((a) => ({ id: a.clinicId, clinicName: a.clinicName, clinicAddress: a.clinicAddress }));

  function loadAssociations() {
    setLoading(true);
    getMyClinicAssociations()
      .then((list) => {
        setAssociations(list);
        const approved = list.filter((a) => a.status === "APPROVED");
        setAvailForm((f) => (f.clinicId || !approved.length ? f : { ...f, clinicId: approved[0].clinicId }));
      })
      .catch(() => setError("Couldn't load your clinics."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadAssociations();
    loadAvailability();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function updateAvailForm(field) {
    return (e) => setAvailForm({ ...availForm, [field]: e.target.value });
  }

  async function handleBrowse(e) {
    e?.preventDefault();
    setBrowsing(true);
    setError("");
    try {
      const results = await browseClinics(cityFilter || undefined);
      setBrowseResults(results);
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't load clinics to browse.");
    } finally {
      setBrowsing(false);
    }
  }

  async function handleRequestJoin(clinicId) {
    setBusyId(clinicId);
    setError("");
    setMessage("");
    try {
      await requestJoinClinic(clinicId);
      setMessage("Join request sent. The clinic will need to approve it before you can set hours there.");
      loadAssociations();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't send that join request.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleRespond(associationId, approve) {
    setBusyId(associationId);
    setError("");
    setMessage("");
    try {
      if (approve) {
        await approveClinicInvite(associationId);
        setMessage("Invite accepted. You can now set your hours there.");
      } else {
        await rejectClinicInvite(associationId);
        setMessage("Invite declined.");
      }
      loadAssociations();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't update that invite.");
    } finally {
      setBusyId(null);
    }
  }

  async function handleLeave(associationId) {
    setBusyId(associationId);
    setError("");
    setMessage("");
    try {
      await leaveClinic(associationId);
      setMessage("You've left that clinic.");
      loadAssociations();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't leave that clinic.");
    } finally {
      setBusyId(null);
    }
  }

  async function submitAvailability(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    setSavingAvail(true);
    try {
      await addAvailability({
        ...availForm,
        clinicId: Number(availForm.clinicId),
        slotDurationMinutes: Number(availForm.slotDurationMinutes),
      });
      setMessage("Weekly availability added.");
      loadAvailability();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't add availability.");
    } finally {
      setSavingAvail(false);
    }
  }

  async function handleDeleteAvailability(availabilityId) {
    setDeletingAvailId(availabilityId);
    setError("");
    setMessage("");
    try {
      await deleteAvailability(availabilityId);
      setMessage("Availability removed.");
      loadAvailability();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't remove that availability.");
    } finally {
      setDeletingAvailId(null);
    }
  }

  const pendingInvitesFromClinic = associations.filter((a) => a.status === "PENDING" && a.initiatedBy === "CLINIC");
  const pendingRequestsFromMe = associations.filter((a) => a.status === "PENDING" && a.initiatedBy === "DOCTOR");
  const pastAssociations = associations.filter((a) => a.status === "REJECTED" || a.status === "REMOVED");
  const browsableIds = new Set(associations.filter((a) => a.status !== "REMOVED" && a.status !== "REJECTED").map((a) => a.clinicId));

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>Clinics & Availability</h2>
      </div>

      {error && <div className="form-error">{error}</div>}
      {message && <div className="banner-success">{message}</div>}

      {!loading && pendingInvitesFromClinic.length > 0 && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h3>Clinics inviting you</h3>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Clinic</th>
                  <th>Address</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingInvitesFromClinic.map((a) => (
                  <tr key={a.id}>
                    <td>{a.clinicName}</td>
                    <td>{a.clinicAddress}</td>
                    <td className="actions">
                      <button
                        className="btn btn-primary btn-sm"
                        disabled={busyId === a.id}
                        onClick={() => handleRespond(a.id, true)}
                      >
                        Accept
                      </button>
                      <button
                        className="btn btn-secondary btn-sm"
                        disabled={busyId === a.id}
                        onClick={() => handleRespond(a.id, false)}
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

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>My clinics</h3>
        {loading ? (
          <p>Loading...</p>
        ) : approvedClinics.length === 0 && pendingRequestsFromMe.length === 0 ? (
          <p>You're not associated with any clinic yet. Browse clinics below and request to join one.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Clinic</th>
                  <th>Address</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {[...approvedClinics.map((c) => associations.find((a) => a.clinicId === c.id)), ...pendingRequestsFromMe]
                  .filter(Boolean)
                  .map((a) => (
                    <tr key={a.id}>
                      <td>{a.clinicName}</td>
                      <td>{a.clinicAddress}</td>
                      <td>
                        <span className={`badge ${statusBadgeClass(a.status)}`}>{a.status}</span>
                      </td>
                      <td className="actions">
                        {a.status === "APPROVED" && (
                          <button
                            className="btn btn-secondary btn-sm"
                            disabled={busyId === a.id}
                            onClick={() => handleLeave(a.id)}
                          >
                            Leave
                          </button>
                        )}
                        {a.status === "PENDING" && <span style={{ color: "var(--ink-soft)" }}>Awaiting clinic approval</span>}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && pastAssociations.length > 0 && (
          <p style={{ marginTop: 12, fontSize: "0.85rem", color: "var(--ink-soft)" }}>
            {pastAssociations.length} past association{pastAssociations.length === 1 ? "" : "s"} (rejected/left) not
            shown above.
          </p>
        )}
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>Browse clinics to join</h3>
        <form onSubmit={handleBrowse} className="form-row" style={{ alignItems: "flex-end" }}>
          <div className="field" style={{ flex: 1 }}>
            <label htmlFor="city-filter">City (optional)</label>
            <input
              id="city-filter"
              placeholder="e.g. Bengaluru"
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
            />
          </div>
          <button className="btn btn-primary" disabled={browsing} type="submit">
            {browsing ? "Searching..." : "Search clinics"}
          </button>
        </form>

        {browseResults.length > 0 && (
          <div className="table-wrap" style={{ marginTop: 16 }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Clinic</th>
                  <th>Address</th>
                  <th>Doctors</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {browseResults.map((c) => (
                  <tr key={c.id}>
                    <td>{c.clinicName}</td>
                    <td>
                      {c.address}
                      {c.city ? `, ${c.city}` : ""}
                    </td>
                    <td>{c.doctorCount}</td>
                    <td className="actions">
                      {browsableIds.has(c.id) ? (
                        <span style={{ color: "var(--ink-soft)" }}>Already connected</span>
                      ) : (
                        <button
                          className="btn btn-primary btn-sm"
                          disabled={busyId === c.id}
                          onClick={() => handleRequestJoin(c.id)}
                        >
                          Request to join
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>My weekly hours</h3>
        {availLoading ? (
          <p>Loading...</p>
        ) : myAvailability.length === 0 ? (
          <p>You haven't set any working hours yet. Add some below.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Clinic</th>
                  <th>Day</th>
                  <th>Time</th>
                  <th>Slot length</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {myAvailability.map((a) => (
                  <tr key={a.id}>
                    <td>{a.clinicName}</td>
                    <td>{labelDay(a.dayOfWeek)}</td>
                    <td>
                      {a.startTime}–{a.endTime}
                    </td>
                    <td>{a.slotDurationMinutes} min</td>
                    <td>
                      <span className={`badge ${a.active ? "badge-success" : "badge-muted"}`}>
                        {a.active ? "Active" : "Turned off by clinic"}
                      </span>
                    </td>
                    <td className="actions">
                      <button
                        className="btn btn-secondary btn-sm"
                        disabled={deletingAvailId === a.id}
                        onClick={() => handleDeleteAvailability(a.id)}
                      >
                        {deletingAvailId === a.id ? "Removing..." : "Remove"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!availLoading && myAvailability.some((a) => !a.active) && (
          <p style={{ marginTop: 12, fontSize: "0.85rem", color: "var(--ink-soft)" }}>
            Hours marked "Turned off by clinic" were switched off by that clinic's admin and won't be bookable
            until they're turned back on.
          </p>
        )}
      </div>

      <div className="card">
        <h3>Add weekly availability</h3>
        {loading ? (
          <p>Loading...</p>
        ) : approvedClinics.length === 0 ? (
          <p>You need an approved clinic association before you can set working hours. Join a clinic above first.</p>
        ) : (
          <form onSubmit={submitAvailability}>
            <div className="form-row">
              <div className="field">
                <label htmlFor="a-clinic">Clinic</label>
                <select id="a-clinic" value={availForm.clinicId} onChange={updateAvailForm("clinicId")}>
                  {approvedClinics.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.clinicName}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="a-day">Day</label>
                <select id="a-day" value={availForm.dayOfWeek} onChange={updateAvailForm("dayOfWeek")}>
                  {DAYS.map((d) => (
                    <option key={d} value={d}>
                      {labelDay(d)}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="form-row">
              <div className="field">
                <label htmlFor="a-start">Start time</label>
                <input id="a-start" type="time" value={availForm.startTime} onChange={updateAvailForm("startTime")} />
              </div>
              <div className="field">
                <label htmlFor="a-end">End time</label>
                <input id="a-end" type="time" value={availForm.endTime} onChange={updateAvailForm("endTime")} />
              </div>
              <div className="field">
                <label htmlFor="a-duration">Slot length (min)</label>
                <input
                  id="a-duration"
                  type="number"
                  min="5"
                  step="5"
                  value={availForm.slotDurationMinutes}
                  onChange={updateAvailForm("slotDurationMinutes")}
                />
              </div>
            </div>
            <button className="btn btn-primary btn-block" disabled={savingAvail} type="submit">
              {savingAvail ? "Saving..." : "Add availability"}
            </button>
          </form>
        )}
      </div>
    </DashboardLayout>
  );
}
