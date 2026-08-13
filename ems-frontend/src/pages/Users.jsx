import { useEffect, useState, useCallback } from 'react';
import { getUsers, createUser } from '../api/users';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const ROLES = ['ADMIN', 'MANAGER', 'EMPLOYEE'];
const emptyForm = { username: '', email: '', password: '', roleName: 'EMPLOYEE', enabled: true };

export default function Users() {
  const [users, setUsers] = useState([]);
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

  async function handleSubmit(e) {
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

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ New User</button>
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
            <div className="form-grid">
              <label>
                Username
                <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required minLength={3} />
              </label>
              <label>
                Email
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
              </label>
              <label>
                Password
                <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={6} />
              </label>
              <label>
                Role
                <select value={form.roleName} onChange={(e) => setForm({ ...form, roleName: e.target.value })}>
                  {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </label>
              <label className="checkbox-label">
                <input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
                Enabled
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setShowForm(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Create</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
