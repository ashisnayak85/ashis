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
import EmployeeTypes from './pages/EmployeeTypes'
import Attendance from './pages/Attendance';
import Leaves from './pages/Leaves';
import Resignations from './pages/Resignations';
import HiringDashboard from './pages/hiring/HiringDashboard';
import Requisitions from './pages/hiring/Requisitions';
import RequisitionDetail from './pages/hiring/RequisitionDetail';
import Candidates from './pages/hiring/Candidates';
import CandidateDetail from './pages/hiring/CandidateDetail';
import ApplicationDetail from './pages/hiring/ApplicationDetail';
import MyInterviews from './pages/hiring/MyInterviews';
import HiringApprovals from './pages/hiring/HiringApprovals';
import Offers from './pages/hiring/Offers';
import OfferDetail from './pages/hiring/OfferDetail';
import Tickets from './pages/Tickets';
import TicketDetail from './pages/TicketDetail';
import TicketSetup from './pages/TicketSetup';
import Users from './pages/Users';
import WorkflowSetup from './pages/WorkflowSetup';
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
                <Route path="/employee-type" element={<EmployeeTypes />} />
              </Route>
              <Route path="/attendance" element={<Attendance />} />
              <Route path="/leaves" element={<Leaves />} />
              <Route path="/resignations" element={<Resignations />} />
              {/* Hiring: no role gate here on purpose - access depends on the RECORD (owner / approver /
                  recruiter / interviewer), so the backend (HiringAccessService) decides, and the sidebar
                  only shows what the signed-in person can use. */}
              <Route path="/hiring" element={<HiringDashboard />} />
              <Route path="/hiring/requisitions" element={<Requisitions />} />
              <Route path="/hiring/requisitions/:id" element={<RequisitionDetail />} />
              <Route path="/hiring/candidates" element={<Candidates />} />
              <Route path="/hiring/candidates/:id" element={<CandidateDetail />} />
              <Route path="/hiring/applications/:id" element={<ApplicationDetail />} />
              <Route path="/hiring/interviews" element={<MyInterviews />} />
              <Route path="/hiring/approvals" element={<HiringApprovals />} />
              <Route path="/hiring/offers" element={<Offers />} />
              <Route path="/hiring/offers/:id" element={<OfferDetail />} />
              <Route path="/tickets" element={<Tickets />} />
              <Route path="/tickets/:id" element={<TicketDetail />} />
              <Route element={<ProtectedRoute roles={['ADMIN', 'MANAGER']} />}>
                <Route path="/ticket-setup" element={<TicketSetup />} />
              </Route>
              <Route path="/profile" element={<Profile />} />
              <Route element={<ProtectedRoute role="ADMIN" />}>
                <Route path="/admin/users" element={<Users />} />
                <Route path="/admin/workflow-setup" element={<WorkflowSetup />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
