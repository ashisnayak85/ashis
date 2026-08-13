import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ role }) {
  const { user, checking, hasRole } = useAuth();

  if (checking) return <div className="page-loading">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (role && !hasRole(role)) return <Navigate to="/dashboard" replace />;

  return <Outlet />;
}
