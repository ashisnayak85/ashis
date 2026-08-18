import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import { markAttendance, markMyAttendance, searchAttendance } from '../api/attendance';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const STATUS_OPTIONS = ['PRESENT', 'ABSENT', 'HALF_DAY', 'ON_LEAVE'];
// A plain employee marking their own attendance can only say they're in or
// half-day - ABSENT/ON_LEAVE are things HR/a manager records (or that come
// from an approved leave), not something you self-report as "present but absent".
const SELF_STATUS_OPTIONS = ['PRESENT', 'HALF_DAY'];

const SOURCE_LABEL = { SELF: 'Self', ADMIN: 'Admin', BIOMETRIC: 'Biometric' };

const EMPTY_FILTERS = { employeeId: '', startDate: '', endDate: '', status: '' };

export default function Attendance() {
  const { isStaff } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [records, setRecords] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const today = new Date().toISOString().slice(0, 10);

  const [form, setForm] = useState({
    employeeId: '',
    attendanceDate: today,
    checkInTime: '',
    checkOutTime: '',
    status: 'PRESENT',
    remarks: '',
  });

  useEffect(() => {
    if (isStaff) getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, [isStaff]);

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

  async function handleMark(e) {
    e.preventDefault();
    setError('');
    try {
      const payload = {
        status: form.status,
        checkInTime: form.checkInTime || null,
        checkOutTime: form.checkOutTime || null,
        remarks: form.remarks,
      };
      if (isStaff) {
        await markAttendance({ ...payload, employeeId: Number(form.employeeId), attendanceDate: form.attendanceDate });
      } else {
        await markMyAttendance(payload);
      }
      setSuccess('Attendance marked');
      setPage(0);
      loadRecords();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <h1>Attendance</h1>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="two-col">
        <form className="card-form" onSubmit={handleMark}>
          <h2>{isStaff ? 'Mark Attendance' : 'Mark My Attendance'}</h2>
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
            Date
            {isStaff ? (
              <input type="date" value={form.attendanceDate} onChange={(e) => setForm({ ...form, attendanceDate: e.target.value })} required />
            ) : (
              // Self-service is always for today - shown read-only rather than
              // editable, since the backend forces today's date regardless.
              <input type="date" value={today} disabled />
            )}
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
              {(isStaff ? STATUS_OPTIONS : SELF_STATUS_OPTIONS).map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
          <label>
            Remarks
            <input value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
          </label>
          <button className="btn btn-primary" type="submit">Mark</button>
        </form>

        <div className="card-panel">
          <h2>{isStaff ? 'View Attendance' : 'My Attendance History'}</h2>

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
            <button type="button" className="btn btn-link" onClick={clearFilters}>Clear filters</button>
          )}

          {loading && <Loading />}

          {!loading && records && (
            <>
              <table className="data-table">
                <thead>
                  <tr>
                    {isStaff && <th>Employee</th>}
                    <th>Date</th><th>Check In</th><th>Check Out</th><th>Status</th><th>Source</th><th>Remarks</th>
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
                      <td>{SOURCE_LABEL[r.source] || r.source}</td>
                      <td>{r.remarks}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={isStaff ? 7 : 6} className="empty-row">No records</td></tr>
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
    </div>
  );
}
