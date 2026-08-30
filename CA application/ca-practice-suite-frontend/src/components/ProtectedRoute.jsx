import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Accepts a single role ("ADMIN") or a list (["ADMIN", "ACCOUNTANT"]) - the
// route is reachable if the user has ANY of the listed roles.
export default function ProtectedRoute({ role, roles }) {
  const { user, checking, hasAnyRole } = useAuth();
  const required = roles || (role ? [role] : null);

  if (checking) return <div className="page-loading">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (required && !hasAnyRole(required)) return <Navigate to="/dashboard" replace />;

  return <Outlet />;
}
