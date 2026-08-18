import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import { applyLeave, getPendingLeaves, approveLeave, rejectLeave, getMyLeaves } from '../api/leaves';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const LEAVE_TYPES = ['CASUAL', 'SICK', 'EARNED', 'UNPAID'];
const STATUS_OPTIONS = ['PENDING', 'APPROVED', 'REJECTED'];

function StatusBadge({ status }) {
  const cls = status === 'APPROVED' ? 'badge-success' : status === 'REJECTED' ? 'badge-danger' : 'badge-warning';
  return <span className={`badge ${cls}`}>{status}</span>;
}

export default function Leaves() {
  const { isStaff } = useAuth();
  const [employees, setEmployees] = useState([]);

  const [form, setForm] = useState({
    employeeId: '',
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: '',
  });

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);

  // Staff (ADMIN/MANAGER): approvals queue for everyone
  const [pending, setPending] = useState([]);

  // Everyone: their own leave history, optionally filtered by status
  const [myStatus, setMyStatus] = useState('');
  const [myPage, setMyPage] = useState(0);
  const [myLeaves, setMyLeaves] = useState(null);

  useEffect(() => {
    if (isStaff) getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, [isStaff]);

  const loadPending = useCallback(() => {
    if (!isStaff) return;
    getPendingLeaves()
      .then((res) => setPending(res.data || []))
      .catch((err) => setError(err.message));
  }, [isStaff]);

  const loadMyLeaves = useCallback(() => {
    setLoading(true);
    getMyLeaves({ status: myStatus, page: myPage, size: 10 })
      .then((res) => setMyLeaves(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [myStatus, myPage]);

  useEffect(() => { loadPending(); }, [loadPending]);
  useEffect(() => { loadMyLeaves(); }, [loadMyLeaves]);

  async function handleApply(e) {
    e.preventDefault();
    setError('');
    try {
      const dto = { ...form };
      // Employee dropdown only exists for staff - a plain employee's own
      // employeeId is resolved server-side regardless of what's sent.
      if (isStaff) dto.employeeId = Number(form.employeeId);
      await applyLeave(dto);
      setSuccess('Leave application submitted');
      setForm({ employeeId: '', leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' });
      loadPending();
      setMyPage(0);
      loadMyLeaves();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDecision(id, action) {
    setError('');
    try {
      if (action === 'approve') await approveLeave(id);
      else await rejectLeave(id);
      setSuccess(`Leave ${action}d`);
      loadPending();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <h1>Leaves</h1>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="two-col">
        <form className="card-form" onSubmit={handleApply}>
          <h2>Apply for Leave</h2>
          {isStaff && (
            <label>
              Employee
              <select value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} required>
                <option value="" disabled>Select employee</option>
                {employees.map((e) => (
                  <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>
                ))}
              </select>
            </label>
          )}
          <label>
            Leave Type
            <select value={form.leaveType} onChange={(e) => setForm({ ...form, leaveType: e.target.value })}>
              {LEAVE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <label>
            Start Date
            <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
          </label>
          <label>
            End Date
            <input type="date" value={form.endDate} min={form.startDate || undefined} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
          </label>
          <label>
            Reason
            <textarea value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} maxLength={500} />
          </label>
          <button className="btn btn-primary" type="submit">Apply</button>
        </form>

        {isStaff ? (
          <div className="card-panel">
            <h2>Pending Approvals</h2>
            <table className="data-table">
              <thead>
                <tr><th>Employee</th><th>Type</th><th>From</th><th>To</th><th>Reason</th><th></th></tr>
              </thead>
              <tbody>
                {pending.length ? pending.map((l) => (
                  <tr key={l.id}>
                    <td>{l.employeeName}</td>
                    <td>{l.leaveType}</td>
                    <td>{l.startDate}</td>
                    <td>{l.endDate}</td>
                    <td>{l.reason}</td>
                    <td className="row-actions">
                      <button className="btn btn-link" onClick={() => handleDecision(l.id, 'approve')}>Approve</button>
                      <button className="btn btn-link btn-danger" onClick={() => handleDecision(l.id, 'reject')}>Reject</button>
                    </td>
                  </tr>
                )) : (
                  <tr><td colSpan={6} className="empty-row">No pending leaves</td></tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="card-panel">
            <h2>My Leave History</h2>
            <label>
              Status
              <select value={myStatus} onChange={(e) => { setMyPage(0); setMyStatus(e.target.value); }}>
                <option value="">All</option>
                {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>

            {loading ? <Loading /> : myLeaves && (
              <>
                <table className="data-table">
                  <thead>
                    <tr><th>Type</th><th>From</th><th>To</th><th>Reason</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {myLeaves.content?.length ? myLeaves.content.map((l) => (
                      <tr key={l.id}>
                        <td>{l.leaveType}</td>
                        <td>{l.startDate}</td>
                        <td>{l.endDate}</td>
                        <td>{l.reason}</td>
                        <td><StatusBadge status={l.status} /></td>
                      </tr>
                    )) : (
                      <tr><td colSpan={5} className="empty-row">No leaves found</td></tr>
                    )}
                  </tbody>
                </table>
                <Pagination
                  pageNumber={myLeaves.pageNumber}
                  totalPages={myLeaves.totalPages}
                  first={myLeaves.first}
                  last={myLeaves.last}
                  onChange={setMyPage}
                />
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
