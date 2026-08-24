import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyDoctorProfile, getMyDoctorAppointments } from "../../api/doctorDashboard";
import { DOCTOR_NAV_ITEMS } from "./navItems";

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

export default function DoctorDashboard() {
  const [profile, setProfile] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getMyDoctorProfile(), getMyDoctorAppointments()])
      .then(([p, a]) => {
        setProfile(p);
        setAppointments(a);
      })
      .catch(() => setError("Couldn't load your dashboard. Please refresh."))
      .finally(() => setLoading(false));
  }, []);

  const today = todayISO();
  const todayCount = appointments.filter((a) => a.appointmentDate === today && a.status === "BOOKED").length;
  const upcomingCount = appointments.filter((a) => a.status === "BOOKED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;

  return (
    <DashboardLayout title="Doctor Portal" navItems={DOCTOR_NAV_ITEMS}>
      <div className="section-header">
        <h2>Overview</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      {!loading && profile && !profile.verified && (
        <div className="banner-warning">
          Your profile is pending admin verification. You won't appear in patient search until an admin
          approves your account.
        </div>
      )}

      {loading ? (
        <p>Loading...</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-value">{todayCount}</div>
              <div className="stat-label">Today's appointments</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{upcomingCount}</div>
              <div className="stat-label">Upcoming (booked)</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{completedCount}</div>
              <div className="stat-label">Completed</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{profile?.clinics?.length || 0}</div>
              <div className="stat-label">Clinics added</div>
            </div>
          </div>

          <div className="card">
            <h3>Profile summary</h3>
            <p>
              <strong>{profile?.name}</strong> &middot; {profile?.qualification || "Qualification not set"}
            </p>
            <p>
              {profile?.experienceYears ? `${profile.experienceYears} years experience` : "Experience not set"}
              {" · "}
              {profile?.consultationFee != null ? `Rs. ${profile.consultationFee} consultation fee` : "Fee not set"}
            </p>
            <p>
              Specializations:{" "}
              {profile?.specializations?.length ? profile.specializations.join(", ") : "None added yet"}
            </p>
            <span className={`badge ${profile?.verified ? "badge-success" : "badge-warning"}`}>
              {profile?.verified ? "Verified" : "Pending verification"}
            </span>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
