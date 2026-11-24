const APP_VERSION = import.meta.env.VITE_APP_VERSION || '1.0.0';
const ORG_NAME = import.meta.env.VITE_ORG_NAME || 'Enterprise Employee Management System';

export default function Footer() {
  return (
    <footer className="app-footer">
      <span>© {new Date().getFullYear()} {ORG_NAME}. All rights reserved.</span>
      <span className="app-footer-version">v{APP_VERSION}</span>
    </footer>
  );
}
