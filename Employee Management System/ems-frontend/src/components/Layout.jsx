import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Footer from './Footer';
import useHiringMe from './hiring/useHiringMe';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', initial: 'D' },
  { to: '/departments', label: 'Departments', initial: 'P', roles: ['ADMIN', 'MANAGER'] },
  { to: '/designations', label: 'Designations', initial: 'G', roles: ['ADMIN', 'MANAGER'] },
  { to: '/employee-type', label: 'Employee Type', initial: 'ET', roles: ['ADMIN', 'MANAGER'] },
  { to: '/locations', label: 'Locations', initial: 'L', roles: ['ADMIN', 'MANAGER'] },
  { to: '/employees', label: 'Employees', initial: 'E', roles: ['ADMIN', 'MANAGER'] },
  { to: '/all-employees', label: 'All Employees', initial: 'A', roles: ['ADMIN', 'MANAGER'] },
  { to: '/attendance', label: 'Attendance', initial: 'T' },
  { to: '/leaves', label: 'Leave', initial: 'L' },
  { to: '/resignations', label: 'Resignation', initial: 'R' },
  // Hiring: `hiring` says WHEN to show it (see hiringVisible); the backend still enforces access.
  { to: '/hiring', label: 'Hiring Dashboard', initial: 'HD', hiring: 'staff', end: true },
  { to: '/hiring/requisitions', label: 'Requisitions', initial: 'RQ', hiring: 'requisitions' },
  { to: '/hiring/candidates', label: 'Candidates', initial: 'CA', hiring: 'staff' },
  { to: '/hiring/offers', label: 'Offers', initial: 'OF', hiring: 'staff' },
  { to: '/hiring/interviews', label: 'My Interviews', initial: 'MI', hiring: 'all' },
  { to: '/hiring/approvals', label: 'Hiring Approvals', initial: 'HA', hiring: 'approvals' },
  { to: '/tickets', label: 'Tickets', initial: 'K' },
  { to: '/ticket-setup', label: 'Ticket Setup', initial: 'S', roles: ['ADMIN', 'MANAGER'] },
  { to: '/admin/users', label: 'Users', initial: 'U', roles: ['ADMIN'] },
  { to: '/admin/workflow-setup', label: 'Workflow Setup', initial: 'W', roles: ['ADMIN'] },
];

function hiringVisible(rule, me) {
  if (!me) return false;
  switch (rule) {
    case 'staff': return me.staff;
    case 'requisitions': return me.canRaiseRequisition || me.pendingApprovals > 0;
    case 'approvals': return me.staff || me.canRaiseRequisition || me.pendingApprovals > 0;
    default: return true; // 'all'
  }
}

export default function Layout() {
  const { user, logout, hasAnyRole } = useAuth();
  const hiringMe = useHiringMe();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [sidebarPinned, setSidebarPinned] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const popupRef = useRef(null);

  async function handleLogout() {
    setProfileOpen(false);
    await logout();
    navigate('/login');
  }

  function handleToggleSidebar() {
    // On small screens the button opens/closes an off-canvas drawer instead
    // of pinning the hover-expand rail (that behaviour only makes sense on
    // pointer/desktop layouts).
    if (window.matchMedia('(max-width: 900px)').matches) {
      setMobileNavOpen((open) => !open);
    } else {
      setSidebarPinned((pinned) => !pinned);
    }
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
    if (profileOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [profileOpen]);

  // Close the mobile drawer automatically if the viewport is resized up to
  // desktop width, so it doesn't stay "open" and reappear later.
  useEffect(() => {
    function handleResize() {
      if (!window.matchMedia('(max-width: 900px)').matches) {
        setMobileNavOpen(false);
      }
    }
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div className="app-shell">
      <nav className="navbar">
        <button
          type="button"
          className={`sidebar-toggle${sidebarPinned ? ' sidebar-toggle-active' : ''}${mobileNavOpen ? ' sidebar-toggle-active' : ''}`}
          onClick={handleToggleSidebar}
          aria-label="Toggle sidebar"
          aria-expanded={sidebarPinned || mobileNavOpen}
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <div className="navbar-brand">EMS</div>
        <div className="navbar-spacer" />
        <div className="navbar-user" ref={popupRef}>
          <span className="navbar-username">{user?.employeeName || user?.username}</span>
          <button
            type="button"
            className="profile-trigger"
            onClick={() => setProfileOpen((open) => !open)}
            aria-haspopup="true"
            aria-expanded={profileOpen}
            aria-label="Open profile menu"
          >
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 20c0-4.418 3.582-7 8-7s8 2.582 8 7" />
            </svg>
          </button>

          {profileOpen && (
            <div className="profile-popup">
              <div className="profile-popup-header">
                <div className="profile-avatar">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-4.418 3.582-7 8-7s8 2.582 8 7" />
                  </svg>
                </div>
                <div>
                  <div className="profile-popup-name">{user?.username}</div>
                  <div className="profile-popup-roles">{user?.roles?.join(', ')}</div>
                </div>
              </div>
              <div className="profile-popup-body">
                <div className="profile-popup-row">
                  <span className="profile-popup-label">Username</span>
                  <span>{user?.username}</span>
                </div>
                <div className="profile-popup-row">
                  <span className="profile-popup-label">Roles</span>
                  <span>{user?.roles?.join(', ') || '—'}</span>
                </div>
              </div>
              <div className="profile-popup-actions">
                <NavLink to="/profile" className="btn btn-link" onClick={() => setProfileOpen(false)}>
                  View full profile
                </NavLink>
                <button className="btn btn-danger" onClick={handleLogout}>Logout</button>
              </div>
            </div>
          )}
        </div>
      </nav>

      <div className="app-body">
        {mobileNavOpen && (
          <div className="sidebar-overlay" onClick={() => setMobileNavOpen(false)} />
        )}
        <aside className={`sidebar${sidebarPinned ? ' sidebar-pinned' : ''}${mobileNavOpen ? ' sidebar-mobile-open' : ''}`}>
          <div className="sidebar-links">
            {NAV_ITEMS.filter((item) => (item.hiring
              ? hiringVisible(item.hiring, hiringMe)
              : !item.roles || hasAnyRole(item.roles))).map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className="sidebar-link"
                title={item.label}
                onClick={() => setMobileNavOpen(false)}
              >
                <span className="sidebar-link-icon">{item.initial}</span>
                <span className="sidebar-link-label">{item.label}</span>
              </NavLink>
            ))}
          </div>
        </aside>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
      <Footer />
    </div>
  );
}
