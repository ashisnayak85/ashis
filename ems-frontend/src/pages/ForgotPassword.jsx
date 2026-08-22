import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import { ErrorBanner, SuccessBanner } from '../components/Feedback';

export default function ForgotPassword() {
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await forgotPassword(usernameOrEmail.trim());
      // Always show the same success state, whether or not the account
      // exists - the backend intentionally never reveals that either way.
      setSubmitted(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Forgot Password</h1>
        <p className="auth-subtitle">Enter your username or email and we'll send you a reset link.</p>
        <ErrorBanner message={error} />

        {submitted ? (
          <>
            <SuccessBanner message="If an account matches that username/email, a password reset link has been sent. Check your inbox." />
            <NavLink to="/login" className="btn btn-primary" style={{ textAlign: 'center' }}>Back to login</NavLink>
          </>
        ) : (
          <>
            <label>
              Username or email
              <input value={usernameOrEmail} onChange={(e) => setUsernameOrEmail(e.target.value)} required autoFocus />
            </label>
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Sending…' : 'Send reset link'}
            </button>
            <NavLink to="/login" className="auth-secondary-link">Back to login</NavLink>
          </>
        )}
      </form>
    </div>
  );
}
