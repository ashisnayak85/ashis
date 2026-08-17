import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { changePassword } from '../api/auth';
import { ErrorBanner, SuccessBanner } from '../components/Feedback';

export default function Profile() {
  const { user } = useAuth();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (form.newPassword !== form.confirmPassword) {
      setError('New password and confirmation do not match');
      return;
    }
    if (form.newPassword.length < 6) {
      setError('New password must be at least 6 characters');
      return;
    }

    setSubmitting(true);
    try {
      await changePassword(form.currentPassword, form.newPassword);
      setSuccess('Password updated successfully');
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(err.message || 'Failed to update password');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <h1>Profile</h1>
      <div className="card-panel">
        <p><strong>Username:</strong> {user?.username}</p>
        <p><strong>Roles:</strong> {user?.roles?.join(', ')}</p>
      </div>

      <h2>Change password</h2>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <form className="card-panel" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            Current password
            <input
              type="password"
              value={form.currentPassword}
              onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
              required
            />
          </label>
          <label>
            New password
            <input
              type="password"
              value={form.newPassword}
              onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
              required
              minLength={6}
            />
          </label>
          <label>
            Confirm new password
            <input
              type="password"
              value={form.confirmPassword}
              onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              required
              minLength={6}
            />
          </label>
        </div>
        <div className="modal-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : 'Update password'}
          </button>
        </div>
      </form>
    </div>
  );
}
