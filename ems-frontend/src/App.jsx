import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Dashboard from './pages/Dashboard';
import EmployeeList from './pages/EmployeeList';
import AllEmployeeList from './pages/AllEmployeeList';
import Departments from './pages/Departments';
import Designations from './pages/Designations';
import Locations from './pages/Locations';
import Attendance from './pages/Attendance';
import Leaves from './pages/Leaves';
import Tickets from './pages/Tickets';
import TicketDetail from './pages/TicketDetail';
import TicketSetup from './pages/TicketSetup';
import Users from './pages/Users';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route element={<ProtectedRoute roles={['ADMIN', 'MANAGER']} />}>
                <Route path="/employees" element={<EmployeeList />} />
                <Route path="/all-employees" element={<AllEmployeeList />} />
                <Route path="/departments" element={<Departments />} />
                <Route path="/designations" element={<Designations />} />
                <Route path="/locations" element={<Locations />} />
              </Route>
              <Route path="/attendance" element={<Attendance />} />
              <Route path="/leaves" element={<Leaves />} />
              <Route path="/tickets" element={<Tickets />} />
              <Route path="/tickets/:id" element={<TicketDetail />} />
              <Route element={<ProtectedRoute roles={['ADMIN', 'MANAGER']} />}>
                <Route path="/ticket-setup" element={<TicketSetup />} />
              </Route>
              <Route path="/profile" element={<Profile />} />
              <Route element={<ProtectedRoute role="ADMIN" />}>
                <Route path="/admin/users" element={<Users />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
