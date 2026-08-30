import { useEffect, useState, useCallback } from 'react';
import { getUsers, getRoles, createUser, setUserEnabled } from '../api/users';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const emptyForm = { username: '', password: '', email: '', fullName: '', roles: ['ACCOUNTANT'] };

export default function Users() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(() => {
    setLoading(true);
    getUsers()
      .then((res) => setUsers(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { getRoles().then((res) => setRoles(res.data || [])).catch(() => {}); }, []);

  function toggleRole(roleName) {
    setForm((f) => ({
      ...f,
      roles: f.roles.includes(roleName) ? f.roles.filter((r) => r !== roleName) : [...f.roles, roleName],
    }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    try {
      await createUser(form);
      setSuccess('User created');
      setForm(emptyForm);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleToggleEnabled(u) {
    try {
      await setUserEnabled(u.id, !u.enabled);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
        <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>+ New User</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {showForm && (
        <form className="inline-panel" onSubmit={handleCreate}>
          <div className="form-grid">
            <label className="form-field">
              <span>Username *</span>
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Password *</span>
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Email *</span>
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Full Name *</span>
              <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
            </label>
            <div className="form-field form-field-wide">
              <span>Roles</span>
              <div className="checkbox-row">
                {(roles.length ? roles.map((r) => r.name.replace('ROLE_', '')) : ['ADMIN', 'ACCOUNTANT']).map((r) => (
                  <label key={r} className="form-checkbox">
                    <input type="checkbox" checked={form.roles.includes(r)} onChange={() => toggleRole(r)} />
                    <span>{r}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary">Create User</button>
          </div>
        </form>
      )}

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr><th>Username</th><th>Full Name</th><th>Email</th><th>Roles</th><th>Enabled</th><th></th></tr>
          </thead>
          <tbody>
            {users.length ? users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.roles?.map((r) => r.replace('ROLE_', '')).join(', ')}</td>
                <td>{u.enabled ? 'Yes' : 'No'}</td>
                <td className="row-actions">
                  <button className="btn btn-link" onClick={() => handleToggleEnabled(u)}>
                    {u.enabled ? 'Disable' : 'Enable'}
                  </button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={6} className="empty-state">No users found</td></tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
