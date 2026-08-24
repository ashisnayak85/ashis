import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useSidebar } from "../context/SidebarContext";

export default function Navbar() {
  const { user, logout } = useAuth();
  const { pinned, toggle, setHover } = useSidebar();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  const hasDashboard = user?.role === "DOCTOR" || user?.role === "ADMIN" || user?.role === "CLINIC_ADMIN";

  const dashboardLink =
    user?.role === "DOCTOR"
      ? { to: "/doctor", label: "Dashboard" }
      : user?.role === "ADMIN"
      ? { to: "/admin", label: "Dashboard" }
      : user?.role === "CLINIC_ADMIN"
      ? { to: "/clinic-admin", label: "Dashboard" }
      : { to: "/my-appointments", label: "My appointments" };

  return (
    <nav className="navbar">
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        {hasDashboard && (
          <button
            className="sidebar-toggle-btn"
            aria-label="Toggle menu"
            aria-expanded={pinned}
            onClick={toggle}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
          >
            <span />
            <span />
            <span />
          </button>
        )}
        <Link to="/" className="brand">NearbyCare</Link>
      </div>
      <div className="nav-links">
        {user ? (
          <>
            <Link to={dashboardLink.to}>{dashboardLink.label}</Link>
            <span style={{ color: "var(--ink-soft)" }}>Hi, {user.name}</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register" className="btn btn-primary">Sign up</Link>
          </>
        )}
      </div>
    </nav>
  );
}
