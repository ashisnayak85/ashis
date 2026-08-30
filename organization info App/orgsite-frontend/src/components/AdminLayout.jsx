import { NavLink, useNavigate } from "react-router-dom";
import { clearSession, getOrganizationSlug } from "../api/auth";

export default function AdminLayout({ children }) {
  const navigate = useNavigate();
  const slug = getOrganizationSlug();

  function handleLogout() {
    clearSession();
    navigate("/login");
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">OrgSite</div>
        <nav className="admin-nav">
          <NavLink to="/admin" end className={({ isActive }) => (isActive ? "active" : "")}>
            Business Profile
          </NavLink>
          <NavLink to="/admin/content" className={({ isActive }) => (isActive ? "active" : "")}>
            Content
          </NavLink>
          {slug && (
            <a href={`/${slug}`} target="_blank" rel="noreferrer">
              View public page ↗
            </a>
          )}
        </nav>
        <button className="btn-secondary logout-btn" onClick={handleLogout}>
          Log out
        </button>
      </aside>
      <main className="admin-main">{children}</main>
    </div>
  );
}
