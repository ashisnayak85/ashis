import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { SidebarProvider } from "./context/SidebarContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Navbar from "./components/Navbar";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import DoctorProfile from "./pages/DoctorProfile";
import MyAppointments from "./pages/MyAppointments";
import DoctorDashboard from "./pages/doctor/DoctorDashboard";
import DoctorClinic from "./pages/doctor/DoctorClinic";
import DoctorAppointments from "./pages/doctor/DoctorAppointments";
import DoctorPatients from "./pages/doctor/DoctorPatients";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminDoctors from "./pages/admin/AdminDoctors";
import AdminClinics from "./pages/admin/AdminClinics";
import AdminPatients from "./pages/admin/AdminPatients";
import AdminAppointments from "./pages/admin/AdminAppointments";
import ClinicAdminDashboard from "./pages/clinic-admin/ClinicAdminDashboard";
import ClinicAdminClinics from "./pages/clinic-admin/ClinicAdminClinics";
import ClinicAdminDoctors from "./pages/clinic-admin/ClinicAdminDoctors";

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
            <Route path="/doctors/:doctorId" element={<DoctorProfile />} />
            <Route
              path="/my-appointments"
              element={
                <ProtectedRoute roles={["PATIENT"]}>
                  <MyAppointments />
                </ProtectedRoute>
              }
            />

            <Route
              path="/doctor"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor/clinic"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorClinic />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor/appointments"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorAppointments />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor/patients"
              element={
                <ProtectedRoute roles={["DOCTOR"]}>
                  <DoctorPatients />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin"
              element={
                <ProtectedRoute roles={["ADMIN"]}>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/doctors"
              element={
                <ProtectedRoute roles={["ADMIN"]}>
                  <AdminDoctors />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/clinics"
              element={
                <ProtectedRoute roles={["ADMIN"]}>
                  <AdminClinics />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/patients"
              element={
                <ProtectedRoute roles={["ADMIN"]}>
                  <AdminPatients />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/appointments"
              element={
                <ProtectedRoute roles={["ADMIN"]}>
                  <AdminAppointments />
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
              path="/clinic-admin/clinics"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminClinics />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clinic-admin/doctors"
              element={
                <ProtectedRoute roles={["CLINIC_ADMIN"]}>
                  <ClinicAdminDoctors />
                </ProtectedRoute>
              }
            />
          </Routes>
        </BrowserRouter>
      </SidebarProvider>
    </AuthProvider>
  );
}
