import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBanner } from '../components/Feedback';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await login(username, password);
      if (res.success) {
        navigate(location.state?.from || '/dashboard', { replace: true });
      } else {
        setError(res.message || 'Invalid username or password');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1 className="auth-title">CA Practice Suite</h1>
        <p className="auth-subtitle">Client ledger, invoicing &amp; compliance calendar</p>
        <ErrorBanner message={error} />
        <label className="form-field">
          <span>Username</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label className="form-field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-primary btn-block" disabled={submitting} type="submit">
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="auth-hint">Default: admin / Admin@123</p>
      </form>
    </div>
  );
}
