import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../../components/DashboardLayout";
import { getMyClinics, getAllAssociations } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";

export default function ClinicAdminDashboard() {
  const [clinics, setClinics] = useState([]);
  const [associations, setAssociations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getMyClinics(), getAllAssociations()])
      .then(([c, a]) => {
        setClinics(c);
        setAssociations(a);
      })
      .catch(() => setError("Couldn't load your dashboard. Please refresh."))
      .finally(() => setLoading(false));
  }, []);

  const pendingRequests = associations.filter((a) => a.status === "PENDING" && a.initiatedBy === "DOCTOR");
  const approvedDoctors = associations.filter((a) => a.status === "APPROVED").length;
  const pendingClinics = clinics.filter((c) => !c.verified).length;

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Overview</h2>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : (
        <>
          {clinics.length === 0 && (
            <div className="banner-warning">
              You haven't added a clinic yet. <Link to="/clinic-admin/clinics">Add your first clinic</Link> to start
              inviting doctors.
            </div>
          )}
          {pendingClinics > 0 && (
            <div className="banner-warning">
              {pendingClinics} of your clinics {pendingClinics === 1 ? "is" : "are"} still awaiting admin
              verification and won't appear in patient search or accept doctors until then.
            </div>
          )}

          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-value">{clinics.length}</div>
              <div className="stat-label">Clinics</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{approvedDoctors}</div>
              <div className="stat-label">Doctors associated</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{pendingRequests.length}</div>
              <div className="stat-label">Join requests waiting on you</div>
            </div>
          </div>

          {pendingRequests.length > 0 && (
            <div className="card">
              <h3>Doctors requesting to join</h3>
              <p>
                These doctors asked to join one of your clinics. Review and respond from{" "}
                <Link to="/clinic-admin/doctors">the Doctors tab</Link>.
              </p>
              <ul>
                {pendingRequests.map((r) => (
                  <li key={r.id}>
                    <strong>{r.doctorName}</strong> wants to join <strong>{r.clinicName}</strong>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </DashboardLayout>
  );
}
