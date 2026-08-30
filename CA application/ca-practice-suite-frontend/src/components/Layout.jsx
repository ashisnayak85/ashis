import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', initial: 'D' },
  { to: '/clients', label: 'Clients', initial: 'C' },
  { to: '/ledger', label: 'Ledger', initial: 'L' },
  { to: '/invoices', label: 'Invoices', initial: 'I' },
  { to: '/compliance', label: 'Compliance', initial: 'K' },
  { to: '/accounts', label: 'Chart of Accounts', initial: 'A', roles: ['ADMIN'] },
  { to: '/admin/users', label: 'Users', initial: 'U', roles: ['ADMIN'] },
];

export default function Layout() {
  const { user, logout, hasAnyRole } = useAuth();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const popupRef = useRef(null);

  async function handleLogout() {
    setProfileOpen(false);
    await logout();
    navigate('/login');
  }

  useEffect(() => {
    function handleClickOutside(event) {
      if (popupRef.current && !popupRef.current.contains(event.target)) {
        setProfileOpen(false);
      }
    }
    function handleEscape(event) {
      if (event.key === 'Escape') {
        setProfileOpen(false);
        setMobileNavOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const visibleNav = NAV_ITEMS.filter((item) => !item.roles || hasAnyRole(item.roles));

  return (
    <div className="app-shell">
      <button className="mobile-nav-toggle" onClick={() => setMobileNavOpen((o) => !o)}>☰</button>
      <aside className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">CA Practice Suite</div>
        <nav className="sidebar-nav">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`}
              onClick={() => setMobileNavOpen(false)}
            >
              <span className="sidebar-link-initial">{item.initial}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <div />
          <div className="profile-menu" ref={popupRef}>
            <button className="profile-trigger" onClick={() => setProfileOpen((o) => !o)}>
              {user?.fullName || user?.username}
            </button>
            {profileOpen && (
              <div className="profile-popup">
                <div className="profile-popup-name">{user?.fullName}</div>
                <div className="profile-popup-roles">{user?.roles?.map((r) => r.replace('ROLE_', '')).join(', ')}</div>
                <button className="btn btn-link" onClick={handleLogout}>Log out</button>
              </div>
            )}
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
