import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import { applyLeave, searchLeaves, approveLeave, rejectLeave, getMyLeaves, exportLeaves, exportMyLeaves } from '../api/leaves';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import ExcelIcon from '../components/ExcelIcon';

const LEAVE_TYPES = ['CASUAL', 'SICK', 'EARNED', 'UNPAID'];
const STATUS_OPTIONS = ['PENDING', 'APPROVED', 'REJECTED'];

const EMPTY_FILTERS = { status: '', from: '', to: '', employeeName: '' };

function StatusBadge({ status }) {
  const cls = status === 'APPROVED' ? 'badge-success' : status === 'REJECTED' ? 'badge-danger' : 'badge-warning';
  return <span className={`badge ${cls}`}>{status}</span>;
}

// Shared by both panels - status select + date-range pair. "children" lets the
// admin panel inject its employee-name field without duplicating the rest.
function FilterBar({ filters, onChange, onClear, children }) {
  return (
    <div className="filter-bar">
      <label>
        Status
        <select value={filters.status} onChange={(e) => onChange({ ...filters, status: e.target.value })}>
          <option value="">All</option>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </label>
      <label>
        From
        <input
          type="date"
          value={filters.from}
          max={filters.to || undefined}
          onChange={(e) => onChange({ ...filters, from: e.target.value })}
        />
      </label>
      <label>
        To
        <input
          type="date"
          value={filters.to}
          min={filters.from || undefined}
          onChange={(e) => onChange({ ...filters, to: e.target.value })}
        />
      </label>
      {children}
      <button type="button" className="btn btn-link filter-clear" onClick={onClear}>Clear filters</button>
    </div>
  );
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

  // Staff (ADMIN/MANAGER): leave requests across everyone, filterable + paginated.
  const [adminFilters, setAdminFilters] = useState(EMPTY_FILTERS);
  const [adminPage, setAdminPage] = useState(0);
  const [adminResults, setAdminResults] = useState(null);
  const [adminLoading, setAdminLoading] = useState(true);

  // Everyone: their own leave history, filterable + paginated (no employee filter - always themselves).
  const [myFilters, setMyFilters] = useState(EMPTY_FILTERS);
  const [myPage, setMyPage] = useState(0);
  const [myLeaves, setMyLeaves] = useState(null);

  const [adminExporting, setAdminExporting] = useState(false);
  const [myExporting, setMyExporting] = useState(false);

  useEffect(() => {
    if (isStaff) getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, [isStaff]);

  const loadAdminResults = useCallback(() => {
    if (!isStaff) return;
    setAdminLoading(true);
    searchLeaves({ ...adminFilters, page: adminPage, size: 10 })
      .then((res) => setAdminResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setAdminLoading(false));
  }, [isStaff, adminFilters, adminPage]);

  const loadMyLeaves = useCallback(() => {
    setLoading(true);
    getMyLeaves({ ...myFilters, page: myPage, size: 10 })
      .then((res) => setMyLeaves(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [myFilters, myPage]);

  useEffect(() => { loadAdminResults(); }, [loadAdminResults]);
  useEffect(() => { loadMyLeaves(); }, [loadMyLeaves]);

  function handleAdminFiltersChange(next) {
    setAdminPage(0);
    setAdminFilters(next);
  }

  function handleMyFiltersChange(next) {
    setMyPage(0);
    setMyFilters(next);
  }

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
      loadAdminResults();
      setMyPage(0);
      loadMyLeaves();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAdminExport() {
    setError('');
    setAdminExporting(true);
    try {
      await exportLeaves(adminFilters);
    } catch (err) {
      setError(err.message);
    } finally {
      setAdminExporting(false);
    }
  }

  async function handleMyExport() {
    setError('');
    setMyExporting(true);
    try {
      await exportMyLeaves(myFilters);
    } catch (err) {
      setError(err.message);
    } finally {
      setMyExporting(false);
    }
  }

  async function handleDecision(id, action) {
    setError('');
    try {
      if (action === 'approve') await approveLeave(id);
      else await rejectLeave(id);
      setSuccess(`Leave ${action}d`);
      loadAdminResults();
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
            <div className="panel-header">
              <h2>Leave Requests</h2>
              <button type="button" className="btn btn-excel" onClick={handleAdminExport} disabled={adminExporting}>
                <ExcelIcon />
                {adminExporting ? 'Exporting…' : 'Export to Excel'}
              </button>
            </div>

            <FilterBar filters={adminFilters} onChange={handleAdminFiltersChange} onClear={() => handleAdminFiltersChange(EMPTY_FILTERS)}>
              <label>
                Employee
                <input
                  type="text"
                  list="leave-employee-suggestions"
                  placeholder="Any employee"
                  value={adminFilters.employeeName}
                  onChange={(e) => handleAdminFiltersChange({ ...adminFilters, employeeName: e.target.value })}
                />
                <datalist id="leave-employee-suggestions">
                  {employees.map((e) => (
                    <option key={e.id} value={`${e.firstName} ${e.lastName}`} />
                  ))}
                </datalist>
              </label>
            </FilterBar>

            {adminLoading ? <Loading /> : adminResults && (
              <>
                <div className="data-table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr><th>Employee</th><th>Type</th><th>From</th><th>To</th><th>Reason</th><th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                      {adminResults.content?.length ? adminResults.content.map((l) => (
                        <tr key={l.id}>
                          <td>{l.employeeName}</td>
                          <td>{l.leaveType}</td>
                          <td>{l.startDate}</td>
                          <td>{l.endDate}</td>
                          <td>{l.reason}</td>
                          <td><StatusBadge status={l.status} /></td>
                          <td className="row-actions">
                            {l.status === 'PENDING' && (
                              <>
                                <button className="btn btn-link" onClick={() => handleDecision(l.id, 'approve')}>Approve</button>
                                <button className="btn btn-link btn-danger" onClick={() => handleDecision(l.id, 'reject')}>Reject</button>
                              </>
                            )}
                          </td>
                        </tr>
                      )) : (
                        <tr><td colSpan={7} className="empty-row">No leave records found for these filters</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
                <Pagination
                  pageNumber={adminResults.pageNumber}
                  totalPages={adminResults.totalPages}
                  first={adminResults.first}
                  last={adminResults.last}
                  onChange={setAdminPage}
                />
              </>
            )}
          </div>
        ) : (
          <div className="card-panel">
            <div className="panel-header">
              <h2>My Leave History</h2>
              <button type="button" className="btn btn-excel" onClick={handleMyExport} disabled={myExporting}>
                <ExcelIcon />
                {myExporting ? 'Exporting…' : 'Export to Excel'}
              </button>
            </div>

            <FilterBar filters={myFilters} onChange={handleMyFiltersChange} onClear={() => handleMyFiltersChange(EMPTY_FILTERS)} />

            {loading ? <Loading /> : myLeaves && (
              <>
                <div className="data-table-scroll">
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
                        <tr><td colSpan={5} className="empty-row">No leaves found for these filters</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
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
