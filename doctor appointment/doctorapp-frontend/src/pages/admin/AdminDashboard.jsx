import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/DashboardLayout";
import { getDashboardStats } from "../../api/admin";
import { ADMIN_NAV_ITEMS } from "./navItems";

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(() => setError("Couldn't load platform stats."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <DashboardLayout title="Admin Portal" navItems={ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Dashboard</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-value">{stats.totalDoctors}</div>
              <div className="stat-label">Total doctors</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.verifiedDoctors}</div>
              <div className="stat-label">Verified doctors</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.pendingDoctors}</div>
              <div className="stat-label">Pending verification</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalPatients}</div>
              <div className="stat-label">Total patients</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalAppointments}</div>
              <div className="stat-label">Total appointments</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.todayAppointments}</div>
              <div className="stat-label">Today's appointments</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalClinics}</div>
              <div className="stat-label">Total clinics</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.pendingClinics}</div>
              <div className="stat-label">Clinics pending verification</div>
            </div>
          </div>

          {stats.pendingDoctors > 0 && (
            <div className="banner-warning">
              {stats.pendingDoctors} doctor{stats.pendingDoctors === 1 ? "" : "s"} waiting on verification. {" "}
              <Link to="/admin/doctors">Review them now</Link>.
            </div>
          )}
          {stats.pendingClinics > 0 && (
            <div className="banner-warning">
              {stats.pendingClinics} clinic{stats.pendingClinics === 1 ? "" : "s"} waiting on verification. {" "}
              <Link to="/admin/clinics">Review them now</Link>.
            </div>
          )}
        </>
      )}
    </DashboardLayout>
  );
}
