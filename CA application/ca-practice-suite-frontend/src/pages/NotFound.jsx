import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="auth-page">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <h1>404</h1>
        <p>That page doesn't exist.</p>
        <Link className="btn btn-primary" to="/dashboard">Back to Dashboard</Link>
      </div>
    </div>
  );
}
