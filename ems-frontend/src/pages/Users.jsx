import { useEffect, useState, useCallback } from 'react';
import { getUsers, createUser, getRoles } from '../api/users';
import { getAvailableForUser } from '../api/employees';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const emptyForm = { employeeId: '', roleName: '' };

export default function Users() {
  const [users, setUsers] = useState([]);
  const [availableEmployees, setAvailableEmployees] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getUsers()
      .then((res) => setUsers(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  // Only active employees who don't already have a login show up here - this is
  // what actually prevents creating a free-floating, unlinked account.
  const loadAvailableEmployees = useCallback(() => {
    getAvailableForUser()
      .then((res) => setAvailableEmployees(res.data || []))
      .catch((err) => setError(err.message));
  }, []);

  // Roles come straight from the DB rather than being hardcoded here, so the
  // dropdown can never send a roleName the backend doesn't actually have.
  const loadRoles = useCallback(() => {
    getRoles()
      .then((res) => {
        const list = res.data || [];
        setRoles(list);
        setForm((f) => (f.roleName ? f : { ...f, roleName: list[0]?.name || '' }));
      })
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openForm() {
    setForm(emptyForm);
    loadAvailableEmployees();
    loadRoles();
    setShowForm(true);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!form.employeeId) {
      setError('Please select an employee');
      return;
    }
    setSubmitting(true);
    try {
      await createUser({ employeeId: Number(form.employeeId), roleName: form.roleName });
      setSuccess('User created - login credentials have been emailed to the employee');
      setForm(emptyForm);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
        <button className="btn btn-primary" onClick={openForm}>+ New User</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr><th>Username</th><th>Email</th><th>Role</th><th>Enabled</th></tr>
          </thead>
          <tbody>
            {users.length ? users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.roleName}</td>
                <td>{u.enabled ? 'Yes' : 'No'}</td>
              </tr>
            )) : (
              <tr><td colSpan={4} className="empty-row">No users found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>New User</h2>
            <p className="form-hint">
              Pick an active employee to grant access to. A username and a one-time
              password are generated automatically and emailed to them - you'll never
              see or set their password here.
            </p>
            <div className="form-grid">
              <label>
                Employee
                <select
                  value={form.employeeId}
                  onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
                  required
                >
                  <option value="" disabled>Select an active employee</option>
                  {availableEmployees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.employeeCode} - {emp.firstName} {emp.lastName} ({emp.email})
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Role
                <select value={form.roleName} onChange={(e) => setForm({ ...form, roleName: e.target.value })}>
                  {roles.map((r) => (
                    <option key={r.id} value={r.name}>{r.description || r.name}</option>
                  ))}
                </select>
              </label>
            </div>
            {availableEmployees.length === 0 && (
              <p className="form-hint">No active employees without a login are available right now.</p>
            )}
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setShowForm(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting || availableEmployees.length === 0}>
                {submitting ? 'Creating…' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
