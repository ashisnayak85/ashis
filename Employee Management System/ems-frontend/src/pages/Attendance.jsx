import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import {
  markAttendance, updateAttendance, punchIn, punchOut, getMyTodayStatus, searchAttendance,
  exportAttendance,
  getFaceStatus, enrollFace,
  getAdminFaceStatus, adminEnrollFace, adminTestVerifyFace, getFaceHistory,
} from '../api/attendance';
import Pagination from '../components/Pagination';
import FaceCapture from '../components/FaceCapture';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import ExcelIcon from '../components/ExcelIcon';

const STATUS_OPTIONS = ['PRESENT', 'ABSENT', 'HALF_DAY', 'ON_LEAVE'];
const SOURCE_LABEL = { SELF: 'Self', ADMIN: 'Admin', BIOMETRIC: 'Biometric' };

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

// The list defaults to just today's records (not the entire history) - "Clear
// filters" drops these dates to show everything.
function defaultFilters() {
  return { employeeId: '', startDate: todayStr(), endDate: todayStr(), status: '' };
}
const EMPTY_FILTERS = { employeeId: '', startDate: '', endDate: '', status: '' };

export default function Attendance() {
  const { isStaff } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [filters, setFilters] = useState(defaultFilters);
  const [records, setRecords] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [exporting, setExporting] = useState(false);

  // --- Admin/Manager: mark attendance for anyone, any date ---
  const [form, setForm] = useState({
    employeeId: '',
    attendanceDate: todayStr(),
    checkInTime: '',
    checkOutTime: '',
    status: 'PRESENT',
    remarks: '',
  });

  // --- Employee self-service: punch in / punch out for today ---
  const [todayStatus, setTodayStatus] = useState(null); // null = not punched in yet
  const [punchRemarks, setPunchRemarks] = useState('');
  const [punching, setPunching] = useState(false);
  const [statusLoaded, setStatusLoaded] = useState(false);

  // --- Face verification (buddy-punching prevention) ---
  // enabled/enrolled come entirely from the backend - this component never
  // hardcodes whether the feature is on, so flipping
  // attendance.face-verification.enabled in the backend's config is enough
  // to change what shows up here, with zero frontend code changes.
  const [faceStatus, setFaceStatus] = useState({ enabled: false, enrolled: false });
  const [faceStatusLoaded, setFaceStatusLoaded] = useState(false);
  // What the camera panel is currently doing: null (hidden), 'enroll',
  // 'punch-in', or 'punch-out'.
  const [capturing, setCapturing] = useState(null);
  const [captureBusy, setCaptureBusy] = useState(false);

  useEffect(() => {
    if (isStaff) getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, [isStaff]);

  const loadFaceStatus = useCallback(() => {
    if (isStaff) return;
    getFaceStatus()
      .then((res) => setFaceStatus(res.data))
      .catch(() => setFaceStatus({ enabled: false, enrolled: false }))
      .finally(() => setFaceStatusLoaded(true));
  }, [isStaff]);

  useEffect(() => { loadFaceStatus(); }, [loadFaceStatus]);

  const loadTodayStatus = useCallback(() => {
    if (isStaff) return;
    getMyTodayStatus()
      .then((res) => setTodayStatus(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setStatusLoaded(true));
  }, [isStaff]);

  useEffect(() => { loadTodayStatus(); }, [loadTodayStatus]);

  const loadRecords = useCallback(() => {
    setLoading(true);
    setError('');
    searchAttendance({ ...filters, page, size: 10 })
      .then((res) => setRecords(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [filters, page]);

  useEffect(() => { loadRecords(); }, [loadRecords]);

  function updateFilter(key, value) {
    setPage(0);
    setFilters((f) => ({ ...f, [key]: value }));
  }

  function clearFilters() {
    setPage(0);
    setFilters(EMPTY_FILTERS);
  }

  const filtersActive = Object.values(filters).some(Boolean);

  async function handleExport() {
    setError('');
    setExporting(true);
    try {
      await exportAttendance(filters);
    } catch (err) {
      setError(err.message);
    } finally {
      setExporting(false);
    }
  }

  async function handleMark(e) {
    e.preventDefault();
    setError('');
    try {
      await markAttendance({
        ...form,
        employeeId: Number(form.employeeId),
        checkInTime: form.checkInTime || null,
        checkOutTime: form.checkOutTime || null,
      });
      setSuccess('Attendance marked');
      setPage(0);
      loadRecords();
    } catch (err) {
      setError(err.message);
    }
  }

  // --- Admin/Manager: correcting an EXISTING record (any date) ---
  // Separate from the form above: this only ever edits a row that already
  // exists, never creates a new one - the sanctioned way to fix a past
  // mistake now that "Mark Attendance" is locked to today only.
  const [editingRecord, setEditingRecord] = useState(null);
  const [editForm, setEditForm] = useState({ status: 'PRESENT', checkInTime: '', checkOutTime: '', remarks: '' });
  const [editSaving, setEditSaving] = useState(false);

  function openEdit(record) {
    setEditingRecord(record);
    setEditForm({
      status: record.status,
      checkInTime: record.checkInTime || '',
      checkOutTime: record.checkOutTime || '',
      remarks: record.remarks || '',
    });
  }

  function cancelEdit() {
    setEditingRecord(null);
  }

  async function saveEdit(e) {
    e.preventDefault();
    setError('');
    setEditSaving(true);
    try {
      await updateAttendance(editingRecord.id, {
        ...editForm,
        checkInTime: editForm.checkInTime || null,
        checkOutTime: editForm.checkOutTime || null,
      });
      setSuccess(`Attendance corrected for ${editingRecord.employeeName} on ${editingRecord.attendanceDate}`);
      setEditingRecord(null);
      loadRecords();
    } catch (err) {
      setError(err.message);
    } finally {
      setEditSaving(false);
    }
  }

  // With face verification OFF (or not yet loaded), punch buttons submit
  // directly with no photo - identical to how this always worked before.
  // With it ON, the buttons instead open the FaceCapture panel and the
  // actual punch only fires once a photo has been captured and confirmed.
  function startPunchIn() {
    if (faceStatus.enabled) {
      if (!faceStatus.enrolled) {
        setError('Please complete face enrollment below before punching in.');
        return;
      }
      setCapturing('punch-in');
      return;
    }
    doPunchIn(null);
  }

  function startPunchOut() {
    if (faceStatus.enabled) {
      if (!faceStatus.enrolled) {
        setError('Please complete face enrollment below before punching out.');
        return;
      }
      setCapturing('punch-out');
      return;
    }
    doPunchOut(null);
  }

  async function doPunchIn(imageBlob) {
    setError('');
    setPunching(true);
    setCaptureBusy(true);
    try {
      const res = await punchIn(punchRemarks.trim(), imageBlob);
      setTodayStatus(res.data);
      setPunchRemarks('');
      setCapturing(null);
      setSuccess(`Punched in at ${res.data.checkInTime}${res.data.faceVerified ? ' (face verified)' : ''}`);
      setPage(0);
      loadRecords();
    } catch (err) {
      setError(err.message);
    } finally {
      setPunching(false);
      setCaptureBusy(false);
    }
  }

  async function doPunchOut(imageBlob) {
    setError('');
    setPunching(true);
    setCaptureBusy(true);
    try {
      const res = await punchOut(punchRemarks.trim(), imageBlob);
      setTodayStatus(res.data);
      setPunchRemarks('');
      setCapturing(null);
      setSuccess(`Punched out at ${res.data.checkOutTime}${res.data.faceVerified ? ' (face verified)' : ''}`);
      setPage(0);
      loadRecords();
    } catch (err) {
      setError(err.message);
    } finally {
      setPunching(false);
      setCaptureBusy(false);
    }
  }

  async function doEnroll(imageBlob) {
    setError('');
    setCaptureBusy(true);
    try {
      await enrollFace(imageBlob);
      setCapturing(null);
      setSuccess('Face enrolled successfully. You can now punch in/out with face verification.');
      loadFaceStatus();
    } catch (err) {
      setError(err.message);
    } finally {
      setCaptureBusy(false);
    }
  }

  function handleCaptureConfirm(blob) {
    if (capturing === 'enroll') doEnroll(blob);
    else if (capturing === 'punch-in') doPunchIn(blob);
    else if (capturing === 'punch-out') doPunchOut(blob);
  }

  // --- Admin/Manager: face management for any employee ---
  // Separate from the self-service state above - an admin picks an employee
  // from a dropdown rather than acting as themselves.
  const [adminEmployeeId, setAdminEmployeeId] = useState('');
  const [adminFaceStatus, setAdminFaceStatus] = useState(null);
  const [adminHistory, setAdminHistory] = useState([]);
  const [adminCapturing, setAdminCapturing] = useState(null); // null | 'enroll' | 'test-verify'
  const [adminBusy, setAdminBusy] = useState(false);
  const [adminTestResult, setAdminTestResult] = useState(null);

  const loadAdminFaceInfo = useCallback((employeeId) => {
    if (!employeeId) { setAdminFaceStatus(null); setAdminHistory([]); return; }
    setAdminTestResult(null);
    getAdminFaceStatus(employeeId).then((res) => setAdminFaceStatus(res.data)).catch((err) => setError(err.message));
    getFaceHistory(employeeId).then((res) => setAdminHistory(res.data || [])).catch(() => setAdminHistory([]));
  }, []);

  useEffect(() => { if (isStaff) loadAdminFaceInfo(adminEmployeeId); }, [isStaff, adminEmployeeId, loadAdminFaceInfo]);

  async function handleAdminEnroll(blob) {
    setError('');
    setAdminBusy(true);
    try {
      await adminEnrollFace(adminEmployeeId, blob);
      setAdminCapturing(null);
      setSuccess('Face enrolled/re-enrolled for this employee. Any previous enrollment was backed up automatically.');
      loadAdminFaceInfo(adminEmployeeId);
    } catch (err) {
      setError(err.message);
    } finally {
      setAdminBusy(false);
    }
  }

  async function handleAdminTestVerify(blob) {
    setError('');
    setAdminBusy(true);
    try {
      const res = await adminTestVerifyFace(adminEmployeeId, blob);
      setAdminTestResult(res.data);
      setAdminCapturing(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setAdminBusy(false);
    }
  }

  function handleAdminCaptureConfirm(blob) {
    if (adminCapturing === 'enroll') handleAdminEnroll(blob);
    else if (adminCapturing === 'test-verify') handleAdminTestVerify(blob);
  }

  return (
    <div>
      <h1>Attendance</h1>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="two-col">
        {isStaff ? (
          <form className="card-form" onSubmit={handleMark}>
            <h2>Mark Attendance</h2>
            <label>
              Employee
              <select value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} required>
                <option value="" disabled>Select employee</option>
                {employees.map((e) => (
                  <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>
                ))}
              </select>
            </label>
            <label>
              Date
              {/* Admin can only mark a NEW attendance record for today - the
                  backend enforces this too. Past-date fixes go through Edit
                  on an existing row in the table instead. */}
              <input type="date" value={todayStr()} disabled />
            </label>
            <label>
              Check In
              <input type="time" value={form.checkInTime} onChange={(e) => setForm({ ...form, checkInTime: e.target.value })} />
            </label>
            <label>
              Check Out
              <input type="time" value={form.checkOutTime} onChange={(e) => setForm({ ...form, checkOutTime: e.target.value })} />
            </label>
            <label>
              Status
              <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
            <label>
              Remarks
              <input value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
            </label>
            <button className="btn btn-primary" type="submit">Mark</button>
          </form>
        ) : (
          <div className="card-form">
            <h2>My Attendance Today</h2>

            {faceStatus.enabled && (
              <p className="page-subtitle">
                Face verification is <strong>on</strong> for this organization.{' '}
                {faceStatusLoaded && !faceStatus.enrolled && 'You need to enroll your face once before you can punch in.'}
              </p>
            )}

            {capturing ? (
              <FaceCapture
                title={
                  capturing === 'enroll' ? 'Enroll your face' :
                  capturing === 'punch-in' ? 'Verify to punch in' : 'Verify to punch out'
                }
                helperText={
                  capturing === 'enroll'
                    ? 'This one-time photo becomes your reference face for every future punch-in/out.'
                    : 'Take a live photo now - it will be matched against your enrolled face before this punch is recorded.'
                }
                confirmLabel={capturing === 'enroll' ? 'Save as my reference face' : 'Confirm & punch'}
                busy={captureBusy}
                onCapture={handleCaptureConfirm}
                onCancel={() => setCapturing(null)}
              />
            ) : !statusLoaded || !faceStatusLoaded ? (
              <Loading />
            ) : (
              <>
                {faceStatus.enabled && !faceStatus.enrolled && (
                  <button type="button" className="btn btn-primary" onClick={() => setCapturing('enroll')}>
                    Enroll My Face
                  </button>
                )}

                {(!faceStatus.enabled || faceStatus.enrolled) && (
                  !todayStatus?.checkInTime ? (
                    <>
                      <p className="page-subtitle">You haven't punched in yet today. The time is recorded by the server the moment you tap Punch In.</p>
                      <label>
                        Remarks (optional)
                        <input value={punchRemarks} onChange={(e) => setPunchRemarks(e.target.value)} placeholder="e.g. Working from client site" />
                      </label>
                      <button className="btn btn-primary" onClick={startPunchIn} disabled={punching}>
                        {punching ? 'Punching in…' : 'Punch In'}
                      </button>
                    </>
                  ) : !todayStatus.checkOutTime ? (
                    <>
                      <p className="page-subtitle">Punched in today at <strong>{todayStatus.checkInTime}</strong>.</p>
                      <label>
                        Remarks (optional)
                        <input value={punchRemarks} onChange={(e) => setPunchRemarks(e.target.value)} placeholder="e.g. Leaving early for appointment" />
                      </label>
                      <button className="btn btn-primary" onClick={startPunchOut} disabled={punching}>
                        {punching ? 'Punching out…' : 'Punch Out'}
                      </button>
                    </>
                  ) : (
                    <p className="page-subtitle">
                      All done for today - in at <strong>{todayStatus.checkInTime}</strong>, out at <strong>{todayStatus.checkOutTime}</strong>
                      {todayStatus.faceVerified && <span className="badge badge-success face-status-badge">Face verified</span>}.
                    </p>
                  )
                )}
              </>
            )}
          </div>
        )}

        <div className="card-panel">
          <div className="panel-header">
            <h2>{isStaff ? 'View Attendance' : 'My Attendance History'}</h2>
            <button type="button" className="btn btn-excel" onClick={handleExport} disabled={exporting}>
              <ExcelIcon />
              {exporting ? 'Exporting…' : 'Export to Excel'}
            </button>
          </div>

          <div className="form-grid">
            {isStaff && (
              <label>
                Employee
                <select value={filters.employeeId} onChange={(e) => updateFilter('employeeId', e.target.value)}>
                  <option value="">All employees</option>
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Attendance Type
              <select value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
                <option value="">All types</option>
                {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
            <label>
              Start Date
              <input
                type="date"
                value={filters.startDate}
                max={filters.endDate || undefined}
                onChange={(e) => updateFilter('startDate', e.target.value)}
              />
            </label>
            <label>
              End Date
              <input
                type="date"
                value={filters.endDate}
                min={filters.startDate || undefined}
                onChange={(e) => updateFilter('endDate', e.target.value)}
              />
            </label>
          </div>

          {filtersActive && (
            <button type="button" className="btn btn-link" onClick={clearFilters}>Clear filters (show all)</button>
          )}

          {isStaff && editingRecord && (
            <form className="card-form edit-attendance-form" onSubmit={saveEdit}>
              <h3>Correct attendance — {editingRecord.employeeName} on {editingRecord.attendanceDate}</h3>
              <div className="form-grid">
                <label>
                  Status
                  <select value={editForm.status} onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}>
                    {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </label>
                <label>
                  Check In
                  <input type="time" value={editForm.checkInTime} onChange={(e) => setEditForm({ ...editForm, checkInTime: e.target.value })} />
                </label>
                <label>
                  Check Out
                  <input type="time" value={editForm.checkOutTime} onChange={(e) => setEditForm({ ...editForm, checkOutTime: e.target.value })} />
                </label>
                <label className="span-2">
                  Remarks
                  <input value={editForm.remarks} onChange={(e) => setEditForm({ ...editForm, remarks: e.target.value })} placeholder="Reason for this correction" />
                </label>
              </div>
              <div className="row-actions">
                <button className="btn btn-primary" type="submit" disabled={editSaving}>
                  {editSaving ? 'Saving…' : 'Save correction'}
                </button>
                <button type="button" className="btn btn-link" onClick={cancelEdit} disabled={editSaving}>Cancel</button>
              </div>
            </form>
          )}

          {loading && <Loading />}

          {!loading && records && (
            <>
              <table className="data-table">
                <thead>
                  <tr>
                    {isStaff && <th>Employee</th>}
                    <th>Date</th><th>Check In</th><th>Check Out</th><th>Status</th><th>Source</th><th>Remarks</th>
                    {isStaff && <th>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {records.content?.length ? records.content.map((r) => (
                    <tr key={r.id}>
                      {isStaff && <td>{r.employeeName}</td>}
                      <td>{r.attendanceDate}</td>
                      <td>{r.checkInTime || '-'}</td>
                      <td>{r.checkOutTime || '-'}</td>
                      <td>{r.status}</td>
                      <td>
                        {SOURCE_LABEL[r.source] || r.source}
                        {r.faceVerified && <span className="badge badge-success face-status-badge">✓ Face</span>}
                      </td>
                      <td>{r.remarks}</td>
                      {isStaff && (
                        <td>
                          <button type="button" className="btn btn-link" onClick={() => openEdit(r)}>Edit</button>
                        </td>
                      )}
                    </tr>
                  )) : (
                    <tr><td colSpan={isStaff ? 8 : 6} className="empty-row">No records</td></tr>
                  )}
                </tbody>
              </table>
              <Pagination
                pageNumber={records.pageNumber}
                totalPages={records.totalPages}
                first={records.first}
                last={records.last}
                onChange={setPage}
              />
            </>
          )}
        </div>
      </div>

      {isStaff && (
        <div className="card-panel face-admin-panel">
          <h2>Face Verification — Admin</h2>
          <p className="page-subtitle">
            Enroll or re-enroll any employee's face, and test a photo against their enrolled
            face to see the actual match score - useful for figuring out why a real punch-in
            was rejected.
          </p>

          <label>
            Employee
            <select value={adminEmployeeId} onChange={(e) => setAdminEmployeeId(e.target.value)}>
              <option value="">Select employee</option>
              {employees.map((e) => (
                <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>
              ))}
            </select>
          </label>

          {adminEmployeeId && (
            <>
              {!adminFaceStatus ? (
                <Loading />
              ) : !adminFaceStatus.enabled ? (
                <p className="page-subtitle">
                  Face verification is currently <strong>disabled</strong> system-wide
                  (attendance.face-verification.enabled=false) - enrollment/testing is still
                  possible here, but punch-in/out won't require it until it's switched on.
                </p>
              ) : (
                <p className="page-subtitle">
                  Enrollment status:{' '}
                  {adminFaceStatus.enrolled
                    ? <span className="badge badge-success">Enrolled</span>
                    : <span className="badge badge-warning">Not enrolled</span>}
                </p>
              )}

              {adminCapturing ? (
                <FaceCapture
                  title={adminCapturing === 'enroll' ? 'Capture reference photo' : 'Capture test photo'}
                  helperText={
                    adminCapturing === 'enroll'
                      ? 'This becomes the employee\'s new reference face. Their previous enrollment (if any) is backed up automatically, not deleted.'
                      : 'This photo is compared against the employee\'s currently enrolled face - nothing is saved and no attendance is affected.'
                  }
                  confirmLabel={adminCapturing === 'enroll' ? 'Save as reference face' : 'Run test'}
                  busy={adminBusy}
                  onCapture={handleAdminCaptureConfirm}
                  onCancel={() => setAdminCapturing(null)}
                />
              ) : (
                <div className="row-actions">
                  <button type="button" className="btn btn-primary" onClick={() => setAdminCapturing('enroll')} disabled={adminBusy}>
                    {adminFaceStatus?.enrolled ? 'Re-enroll Face' : 'Enroll Face'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-link"
                    onClick={() => setAdminCapturing('test-verify')}
                    disabled={adminBusy || !adminFaceStatus?.enrolled}
                  >
                    Test Verify
                  </button>
                </div>
              )}

              {adminTestResult && (
                <div className={`banner ${adminTestResult.matched ? 'banner-success' : 'banner-error'}`}>
                  <strong>{adminTestResult.matched ? 'Match' : 'No match'}</strong>
                  {adminTestResult.similarity != null && (
                    <> — similarity {adminTestResult.similarity.toFixed(3)} (threshold {adminTestResult.threshold.toFixed(3)})</>
                  )}
                  <div>{adminTestResult.message}</div>
                </div>
              )}

              {adminHistory.length > 0 && (
                <>
                  <h3>Enrollment backup history</h3>
                  <table className="data-table">
                    <thead>
                      <tr><th>Originally captured</th><th>Replaced at</th><th>Replaced by</th><th>Photo backed up</th></tr>
                    </thead>
                    <tbody>
                      {adminHistory.map((h) => (
                        <tr key={h.id}>
                          <td>{h.originallyCapturedAt ? new Date(h.originallyCapturedAt).toLocaleString() : '-'}</td>
                          <td>{new Date(h.replacedAt).toLocaleString()}</td>
                          <td>{h.replacedBy}</td>
                          <td>{h.hasPhoto ? 'Yes' : 'No'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
