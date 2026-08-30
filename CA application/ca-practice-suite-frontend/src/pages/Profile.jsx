import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { changePassword } from '../api/auth';
import { ErrorBanner, SuccessBanner } from '../components/Feedback';

export default function Profile() {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await changePassword(currentPassword, newPassword);
      setSuccess('Password changed');
      setCurrentPassword('');
      setNewPassword('');
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header"><h1>Profile</h1></div>
      <div className="panel" style={{ maxWidth: 420 }}>
        <p><strong>{user?.fullName}</strong></p>
        <p className="stat-hint">{user?.username} · {user?.roles?.map((r) => r.replace('ROLE_', '')).join(', ')}</p>

        <h2 style={{ marginTop: '1.5rem' }}>Change Password</h2>
        <ErrorBanner message={error} />
        <SuccessBanner message={success} />
        <form onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Current Password</span>
            <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
          </label>
          <label className="form-field">
            <span>New Password</span>
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} />
          </label>
          <button className="btn btn-primary" type="submit">Update Password</button>
        </form>
      </div>
    </div>
  );
}
