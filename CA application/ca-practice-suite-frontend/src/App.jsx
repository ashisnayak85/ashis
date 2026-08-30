import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Clients from './pages/Clients';
import Ledger from './pages/Ledger';
import Invoices from './pages/Invoices';
import Compliance from './pages/Compliance';
import Accounts from './pages/Accounts';
import Users from './pages/Users';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/clients" element={<Clients />} />
              <Route path="/ledger" element={<Ledger />} />
              <Route path="/invoices" element={<Invoices />} />
              <Route path="/compliance" element={<Compliance />} />
              <Route element={<ProtectedRoute role="ADMIN" />}>
                <Route path="/accounts" element={<Accounts />} />
                <Route path="/admin/users" element={<Users />} />
              </Route>
              <Route path="/profile" element={<Profile />} />
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
