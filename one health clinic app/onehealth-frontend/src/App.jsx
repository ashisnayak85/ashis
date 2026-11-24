import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { SidebarProvider } from "./context/SidebarContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Navbar from "./components/Navbar";

import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import BookAppointment from "./pages/BookAppointment";
import MyAppointments from "./pages/MyAppointments";

import OwnerDashboard from "./pages/owner/OwnerDashboard";
import OwnerClinics from "./pages/owner/OwnerClinics";
import OwnerDoctors from "./pages/owner/OwnerDoctors";
import OwnerSpecializations from "./pages/owner/OwnerSpecializations";
import OwnerEmployees from "./pages/owner/OwnerEmployees";
import OwnerEmployeeDetail from "./pages/owner/OwnerEmployeeDetail";
import OwnerAvailability from "./pages/owner/OwnerAvailability";
import OwnerMedicines from "./pages/owner/OwnerMedicines";
import OwnerStockDispatch from "./pages/owner/OwnerStockDispatch";
import OwnerExpansion from "./pages/owner/OwnerExpansion";

import ClinicAdminDashboard from "./pages/clinic-admin/ClinicAdminDashboard";
import ClinicAdminWalkIn from "./pages/clinic-admin/ClinicAdminWalkIn";
import ClinicAdminAppointments from "./pages/clinic-admin/ClinicAdminAppointments";
import ClinicAdminAvailability from "./pages/clinic-admin/ClinicAdminAvailability";
import ClinicAdminStock from "./pages/clinic-admin/ClinicAdminStock";
import ClinicAdminBilling from "./pages/clinic-admin/ClinicAdminBilling";

import DoctorAppointments from "./pages/doctor/DoctorAppointments";
import DoctorAvailability from "./pages/doctor/DoctorAvailability";

export default function App() {
  return (
    <AuthProvider>
      <SidebarProvider>
        <BrowserRouter>
          <Navbar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            <Route
              path="/book"
              element={
                <ProtectedRoute roles={["PATIENT"]}>
                  <BookAppointment />
                </ProtectedRoute>
              }
            />
            <Route
              path="/my-appointments"
              element={
                <ProtectedRoute roles={["PATIENT"]}>
                  <MyAppointments />
                </ProtectedRoute>
              }
            />

            <Route
              path="/owner"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/clinics"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerClinics />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/doctors"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerDoctors />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/specializations"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerSpecializations />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/employees"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerEmployees />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/employees/:userId"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerEmployeeDetail />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/time-periods"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerAvailability />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/medicines"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerMedicines />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/stock"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerStockDispatch />
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner/expansion"
              element={
                <ProtectedRoute roles={["OWNER"]}>
                  <OwnerExpansion />
                </ProtectedRoute>
              }
            />

            <Route
              path="/clinic-admin"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/walk-in"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminWalkIn />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/appointments"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminAppointments />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/time-periods"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminAvailability />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/stock"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminStock />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/billing"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminBilling />
                </ProtectedRoute>
              }
            />

            <Route
              path="/doctor"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorAppointments />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor/availability"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorAvailability />
                </ProtectedRoute>
              }
            />
          </Routes>
        </BrowserRouter>
      </SidebarProvider>
    </AuthProvider>
  );
}
