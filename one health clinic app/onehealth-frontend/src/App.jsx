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

import ClinicAdminDashboard from "./pages/clinic-admin/ClinicAdminDashboard";
import ClinicAdminWalkIn from "./pages/clinic-admin/ClinicAdminWalkIn";
import ClinicAdminAppointments from "./pages/clinic-admin/ClinicAdminAppointments";

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
