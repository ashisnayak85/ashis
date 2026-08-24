import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useSidebar } from "../context/SidebarContext";

/**
 * The sidebar itself for the Doctor and Admin portals. It's an overlay drawer
 * on every screen size - hidden by default, opened via the hamburger button in
 * the top Navbar (click to pin it open, or just hover it on a mouse/trackpad
 * device for a quick preview). State is shared through SidebarContext since
 * the toggle button and this panel don't share a DOM parent.
 */
export default function DashboardLayout({ title, navItems, children }) {
  const { open, pinned, close, setHover } = useSidebar();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <div className="dashboard-shell">
      <aside
        className={`dashboard-sidebar ${open ? "open" : ""}`}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
      >
        <div className="dashboard-brand">{title}</div>
        <nav className="dashboard-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={close}
              className={({ isActive }) => "dashboard-nav-link" + (isActive ? " active" : "")}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="dashboard-user">
          <span>Signed in as {user?.name}</span>
          <button className="btn btn-secondary btn-block" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      {pinned && <div className="dashboard-overlay" onClick={close} />}

      <main className="dashboard-main">{children}</main>
    </div>
  );
}
