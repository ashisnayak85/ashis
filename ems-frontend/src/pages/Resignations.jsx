import { useEffect, useState, useCallback } from 'react';
import {
  submitResignation, getMyResignation, withdrawResignation, getResignation,
  searchResignations, getPendingApprovalsForMe, approveStage, rejectStage,
  clearDepartment, blockDepartment, addAsset, markAssetReturned,
  finalApprove, overrideLastWorkingDay,
} from '../api/resignations';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const TERMINAL = ['FINAL_APPROVED', 'REJECTED', 'WITHDRAWN'];

function StatusBadge({ status }) {
  const cls = status === 'FINAL_APPROVED' ? 'badge-success'
    : status === 'REJECTED' ? 'badge-danger'
    : status === 'WITHDRAWN' ? 'badge-muted'
    : 'badge-warning';
  return <span className={`badge ${cls}`}>{status?.replaceAll('_', ' ')}</span>;
}

// --- Employee self-service: submit form or current-resignation status card ---
function MyResignationPanel({ onChanged }) {
  const [mine, setMine] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({ reason: '', noticePeriodDays: '' });

  const load = useCallback(() => {
    setLoading(true);
    getMyResignation()
      .then((res) => setMine(res.data))
      .catch(() => setMine(null)) // 404 = no resignation on file yet, that's fine
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await submitResignation(form.reason, form.noticePeriodDays ? Number(form.noticePeriodDays) : null);
      setSuccess('Resignation submitted');
      setForm({ reason: '', noticePeriodDays: '' });
      load();
      onChanged?.();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleWithdraw() {
    setError('');
    try {
      await withdrawResignation(mine.id);
      setSuccess('Resignation withdrawn');
      load();
      onChanged?.();
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <Loading />;

  const hasActive = mine && !TERMINAL.includes(mine.status);

  return (
    <div className="card-panel">
      <h2>My Resignation</h2>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {hasActive ? (
        <ResignationDetail resignation={mine} onChanged={() => { load(); onChanged?.(); }} viewerIsOwner />
      ) : (
        <>
          {mine && (
            <p className="muted-text">
              Your last resignation ended with status <StatusBadge status={mine.status} />.
              You can submit a new one below.
            </p>
          )}
          <form className="card-form" onSubmit={handleSubmit}>
            <label>
              Reason
              <textarea
                value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })}
                maxLength={1000}
                required
              />
            </label>
            <label>
              Notice period (days) — optional
              <input
                type="number"
                min={0}
                placeholder="Defaults to company policy"
                value={form.noticePeriodDays}
                onChange={(e) => setForm({ ...form, noticePeriodDays: e.target.value })}
              />
            </label>
            <button className="btn btn-primary" type="submit">Submit Resignation</button>
          </form>
        </>
      )}
    </div>
  );
}

// --- Approver inbox: resignations awaiting my action at the current stage ---
function PendingApprovalsPanel({ refreshKey, onChanged }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getPendingApprovalsForMe()
      .then((res) => setItems(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load, refreshKey]);

  if (loading) return <Loading />;

  return (
    <div className="card-panel">
      <h2>Pending My Approval</h2>
      <ErrorBanner message={error} />
      {items.length === 0 ? (
        <p className="empty-row">Nothing awaiting your approval right now.</p>
      ) : (
        <div className="data-table-scroll">
          <table className="data-table">
            <thead><tr><th>Employee</th><th>Requested On</th><th>Expected LWD</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {items.map((r) => (
                <tr key={r.id}>
                  <td>{r.employeeName} ({r.employeeCode})</td>
                  <td>{r.requestedOn}</td>
                  <td>{r.expectedLastWorkingDay}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td><button className="btn btn-link" onClick={() => setSelectedId(r.id === selectedId ? null : r.id)}>
                    {selectedId === r.id ? 'Hide' : 'Review'}
                  </button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selectedId && (
        <ResignationDetailLoader id={selectedId} onChanged={() => { load(); onChanged?.(); }} />
      )}
    </div>
  );
}

// --- Admin/HR: search across every resignation ---
function AllResignationsPanel({ refreshKey, onChanged }) {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    searchResignations({ status: status || undefined, page, size: 10 })
      .then((res) => setResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [status, page]);

  useEffect(() => { load(); }, [load, refreshKey]);

  const STATUS_OPTIONS = ['SUBMITTED', 'IN_APPROVAL', 'ALL_APPROVALS_DONE', 'CLEARANCE_PENDING', 'CLEARANCE_DONE', 'FINAL_APPROVED', 'REJECTED', 'WITHDRAWN'];

  return (
    <div className="card-panel">
      <h2>All Resignations</h2>
      <ErrorBanner message={error} />
      <div className="filter-bar">
        <label>
          Status
          <select value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
            <option value="">All</option>
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </select>
        </label>
      </div>

      {loading ? <Loading /> : results && (
        <>
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Employee</th><th>Requested On</th><th>Expected LWD</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {results.content?.length ? results.content.map((r) => (
                  <tr key={r.id}>
                    <td>{r.employeeName} ({r.employeeCode})</td>
                    <td>{r.requestedOn}</td>
                    <td>{r.expectedLastWorkingDay}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td><button className="btn btn-link" onClick={() => setSelectedId(r.id === selectedId ? null : r.id)}>
                      {selectedId === r.id ? 'Hide' : 'View'}
                    </button></td>
                  </tr>
                )) : (
                  <tr><td colSpan={5} className="empty-row">No resignations found for this filter</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <Pagination
            pageNumber={results.pageNumber}
            totalPages={results.totalPages}
            first={results.first}
            last={results.last}
            onChange={setPage}
          />
        </>
      )}

      {selectedId && (
        <ResignationDetailLoader id={selectedId} onChanged={() => { load(); onChanged?.(); }} />
      )}
    </div>
  );
}

// Fetches a single resignation by id and renders the detail view - shared by
// the approver inbox and the admin search panel.
function ResignationDetailLoader({ id, onChanged }) {
  const [resignation, setResignation] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    getResignation(id).then((res) => setResignation(res.data)).catch((err) => setError(err.message));
  }, [id]);

  useEffect(() => { load(); }, [load]);

  if (error) return <ErrorBanner message={error} />;
  if (!resignation) return <Loading />;

  return <ResignationDetail resignation={resignation} onChanged={() => { load(); onChanged?.(); }} />;
}

// The detail view: approval timeline, no-dues clearance, asset return, and
// (when eligible) final approval - all actions gated client-side by role,
// re-enforced server-side regardless.
function ResignationDetail({ resignation: r, onChanged, viewerIsOwner }) {
  const { isStaff, hasRole, user } = useAuth();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [remarks, setRemarks] = useState('');
  const [newAssetDesc, setNewAssetDesc] = useState('');
  const [lwdForm, setLwdForm] = useState({ actualLastWorkingDay: r.actualLastWorkingDay || '', overrideReason: '' });

  async function run(action, successMsg) {
    setError('');
    setSuccess('');
    try {
      await action();
      setSuccess(successMsg);
      onChanged?.();
    } catch (err) {
      setError(err.message);
    }
  }

  const currentStage = r.approvals?.find((a) => a.status === 'PENDING');
  // The "am I the current approver" check is advisory on the client - the
  // server re-checks this on every action regardless.
  const isMyStage = currentStage && user?.employeeId && currentStage.approverEmployeeId === user.employeeId;

  return (
    <div className="resignation-detail">
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="detail-header">
        <div>
          <strong>{r.employeeName}</strong> ({r.employeeCode}) — <StatusBadge status={r.status} />
        </div>
        <div className="muted-text">
          Requested {r.requestedOn} · Notice {r.noticePeriodDays} days · Expected LWD {r.expectedLastWorkingDay}
          {r.actualLastWorkingDay && r.actualLastWorkingDay !== r.expectedLastWorkingDay && (
            <> · Actual LWD <strong>{r.actualLastWorkingDay}</strong>{r.overrideReason ? ` (${r.overrideReason})` : ''}</>
          )}
        </div>
        {r.reason && <div className="muted-text">Reason: {r.reason}</div>}
      </div>

      {viewerIsOwner && !TERMINAL.includes(r.status) && (
        <button className="btn btn-link btn-danger" onClick={() => run(() => withdrawResignation(r.id), 'Resignation withdrawn')}>
          Withdraw Resignation
        </button>
      )}

      <h3>Approval Chain</h3>
      <ol className="approval-timeline">
        {r.approvals?.map((a) => (
          <li key={a.id} className={`approval-step approval-${a.status.toLowerCase()}`}>
            <div><strong>{a.stageName}</strong> — {a.approverName} — <StatusBadge status={a.status} /></div>
            {a.remarks && <div className="muted-text">{a.remarks}</div>}
            {a.actedAt && <div className="muted-text">{new Date(a.actedAt).toLocaleString()}</div>}
          </li>
        ))}
      </ol>

      {isMyStage && (
        <div className="card-form">
          <label>
            Remarks
            <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} maxLength={500} />
          </label>
          <div className="row-actions">
            <button className="btn btn-primary" onClick={() => run(() => approveStage(r.id, remarks), 'Stage approved')}>
              Approve
            </button>
            <button
              className="btn btn-danger"
              onClick={() => remarks.trim() ? run(() => rejectStage(r.id, remarks), 'Resignation rejected') : setError('A reason is required to reject')}
            >
              Reject
            </button>
          </div>
        </div>
      )}

      {r.clearances?.length > 0 && (
        <>
          <h3>No-Dues Clearance</h3>
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Department</th><th>Status</th><th>Cleared By</th><th>Remarks</th>{isStaff && <th></th>}</tr></thead>
              <tbody>
                {r.clearances.map((c) => (
                  <tr key={c.id}>
                    <td>{c.departmentName}</td>
                    <td><StatusBadge status={c.status} /></td>
                    <td>{c.clearedByName || '—'}</td>
                    <td>{c.remarks || '—'}</td>
                    {isStaff && (
                      <td className="row-actions">
                        {c.status === 'PENDING' || c.status === 'BLOCKED' ? (
                          <>
                            <button className="btn btn-link" onClick={() => run(() => clearDepartment(r.id, c.departmentId, remarks), 'Department cleared')}>Clear</button>
                            <button className="btn btn-link btn-danger" onClick={() => run(() => blockDepartment(r.id, c.departmentId, remarks || 'Blocked'), 'Clearance blocked')}>Block</button>
                          </>
                        ) : null}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {(r.status === 'CLEARANCE_PENDING' || r.assets?.length > 0) && (
        <>
          <h3>Assets to Return</h3>
          {isStaff && r.status === 'CLEARANCE_PENDING' && (
            <div className="row-actions">
              <input
                type="text"
                placeholder="e.g. Dell Latitude laptop, tag IT-0231"
                value={newAssetDesc}
                onChange={(e) => setNewAssetDesc(e.target.value)}
              />
              <button
                className="btn btn-link"
                onClick={() => newAssetDesc.trim() && run(() => addAsset(r.id, newAssetDesc), 'Asset item added').then(() => setNewAssetDesc(''))}
              >
                Add Item
              </button>
            </div>
          )}
          {r.assets?.length > 0 && (
            <div className="data-table-scroll">
              <table className="data-table">
                <thead><tr><th>Item</th><th>Returned</th><th>Remarks</th>{isStaff && <th></th>}</tr></thead>
                <tbody>
                  {r.assets.map((a) => (
                    <tr key={a.id}>
                      <td>{a.assetDescription}</td>
                      <td>{a.returned ? 'Yes' : 'No'}</td>
                      <td>{a.remarks || '—'}</td>
                      {isStaff && (
                        <td>
                          {!a.returned && (
                            <button className="btn btn-link" onClick={() => run(() => markAssetReturned(r.id, a.id, remarks), 'Asset marked returned')}>
                              Mark Returned
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {isStaff && !TERMINAL.includes(r.status) && (
        <>
          <h3>Last Working Day Override</h3>
          <div className="row-actions">
            <input
              type="date"
              value={lwdForm.actualLastWorkingDay}
              onChange={(e) => setLwdForm({ ...lwdForm, actualLastWorkingDay: e.target.value })}
            />
            <input
              type="text"
              placeholder="Reason (e.g. buyout, early relieving)"
              value={lwdForm.overrideReason}
              onChange={(e) => setLwdForm({ ...lwdForm, overrideReason: e.target.value })}
            />
            <button
              className="btn btn-link"
              onClick={() => run(() => overrideLastWorkingDay(r.id, lwdForm.actualLastWorkingDay, lwdForm.overrideReason), 'Last working day updated')}
            >
              Save Override
            </button>
          </div>
        </>
      )}

      {hasRole('ADMIN') && r.status === 'CLEARANCE_DONE' && (
        <div className="card-form">
          <p>All approvals and clearances are complete. Final approval will deactivate this employee's account and login.</p>
          <button className="btn btn-primary" onClick={() => run(() => finalApprove(r.id), 'Resignation finally approved - employee relieved')}>
            Final Approve
          </button>
        </div>
      )}
    </div>
  );
}

export default function Resignations() {
  const { isStaff } = useAuth();
  const [refreshKey, setRefreshKey] = useState(0);
  const bump = () => setRefreshKey((k) => k + 1);

  return (
    <div>
      <h1>Resignation / Separation</h1>
      <div className="two-col">
        <MyResignationPanel onChanged={bump} />
        {isStaff && <PendingApprovalsPanel refreshKey={refreshKey} onChanged={bump} />}
      </div>
      {isStaff && (
        <div className="stacked-panel">
          <AllResignationsPanel refreshKey={refreshKey} onChanged={bump} />
        </div>
      )}
    </div>
  );
}
