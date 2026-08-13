import { useEffect, useState, useCallback } from 'react';
import { getEmployees } from '../api/employees';
import { markAttendance, getAttendanceByEmployee } from '../api/attendance';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const STATUS_OPTIONS = ['PRESENT', 'ABSENT', 'HALF_DAY', 'ON_LEAVE'];

export default function Attendance() {
  const [employees, setEmployees] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [records, setRecords] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [form, setForm] = useState({
    employeeId: '',
    attendanceDate: new Date().toISOString().slice(0, 10),
    checkInTime: '',
    checkOutTime: '',
    status: 'PRESENT',
    remarks: '',
  });

  useEffect(() => {
    getEmployees({ page: 0, size: 100 }).then((res) => setEmployees(res.data?.content || []));
  }, []);

  const loadRecords = useCallback(() => {
    if (!selectedEmployee) { setRecords(null); return; }
    setLoading(true);
    getAttendanceByEmployee(selectedEmployee, { page, size: 10 })
      .then((res) => setRecords(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [selectedEmployee, page]);

  useEffect(() => { loadRecords(); }, [loadRecords]);

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
      if (String(form.employeeId) === String(selectedEmployee)) loadRecords();
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
            <input type="date" value={form.attendanceDate} onChange={(e) => setForm({ ...form, attendanceDate: e.target.value })} required />
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

        <div className="card-panel">
          <h2>View Attendance</h2>
          <label>
            Employee
            <select value={selectedEmployee} onChange={(e) => { setPage(0); setSelectedEmployee(e.target.value); }}>
              <option value="">Select employee…</option>
              {employees.map((e) => (
                <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>
              ))}
            </select>
          </label>

          {loading && <Loading />}

          {!loading && records && (
            <>
              <table className="data-table">
                <thead>
                  <tr><th>Date</th><th>Check In</th><th>Check Out</th><th>Status</th><th>Remarks</th></tr>
                </thead>
                <tbody>
                  {records.content?.length ? records.content.map((r) => (
                    <tr key={r.id}>
                      <td>{r.attendanceDate}</td>
                      <td>{r.checkInTime || '-'}</td>
                      <td>{r.checkOutTime || '-'}</td>
                      <td>{r.status}</td>
                      <td>{r.remarks}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={5} className="empty-row">No records</td></tr>
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
