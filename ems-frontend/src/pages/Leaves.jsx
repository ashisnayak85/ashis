import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import { applyLeave, getPendingLeaves, approveLeave, rejectLeave } from '../api/leaves';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const LEAVE_TYPES = ['CASUAL', 'SICK', 'EARNED', 'UNPAID'];

export default function Leaves() {
  const [employees, setEmployees] = useState([]);
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { hasRole } = useAuth();
  const canModerate = hasRole('ADMIN') || hasRole('MANAGER');

  const [form, setForm] = useState({
    employeeId: '',
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: '',
  });

  useEffect(() => {
    getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, []);

  const loadPending = useCallback(() => {
    if (!canModerate) { setLoading(false); return; }
    setLoading(true);
    getPendingLeaves()
      .then((res) => setPending(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [canModerate]);

  useEffect(() => { loadPending(); }, [loadPending]);

  async function handleApply(e) {
    e.preventDefault();
    setError('');
    try {
      await applyLeave({ ...form, employeeId: Number(form.employeeId) });
      setSuccess('Leave application submitted');
      setForm({ employeeId: '', leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' });
      loadPending();
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
            <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
          </label>
          <label>
            Reason
            <textarea value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} maxLength={500} />
          </label>
          <button className="btn btn-primary" type="submit">Apply</button>
        </form>

        {canModerate && (
          <div className="card-panel">
            <h2>Pending Approvals</h2>
            {loading ? <Loading /> : (
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
            )}
          </div>
        )}
      </div>
    </div>
  );
}
