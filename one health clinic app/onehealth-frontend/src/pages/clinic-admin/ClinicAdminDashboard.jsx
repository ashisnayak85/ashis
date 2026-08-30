import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getDashboard } from "../../api/clinicAdmin";
import { CLINIC_ADMIN_NAV_ITEMS } from "./navItems";
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

const OUTCOME_COLORS = ["#2f8f5b", "#c0392b", "#8a5a12", "#5b6b73"];

/** Same shape as the owner dashboard, scoped server-side to this branch only. */
export default function ClinicAdminDashboard() {
  const [from, setFrom] = useState(todayStr());
  const [to, setTo] = useState(todayStr());
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    getDashboard({ from, to })
      .then(setStats)
      .catch(() => setError("Couldn't load dashboard stats."))
      .finally(() => setLoading(false));
  }, [from, to]);

  return (
    <DashboardLayout title="Clinic Portal" navItems={CLINIC_ADMIN_NAV_ITEMS}>
      <div className="section-header">
        <h2>Dashboard</h2>
      </div>

      <div className="filter-bar">
        <div className="field">
          <label>From</label>
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label>To</label>
          <input type="date" value={to} min={from} max={todayStr()} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading || !stats ? (
        <p>Loading...</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-value">{stats.totalAppointments}</div>
              <div className="stat-label">Total appointments</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.overallCompletionRatePercent}%</div>
              <div className="stat-label">Completion rate</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalWalkIns}</div>
              <div className="stat-label">Walk-ins</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalOnlineBookings}</div>
              <div className="stat-label">Online bookings</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalNoShow}</div>
              <div className="stat-label">No-shows</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalUniquePatients}</div>
              <div className="stat-label">Unique patients</div>
            </div>
          </div>

          <div className="chart-grid">
            <div className="chart-card">
              <h3>Outcome split</h3>
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={[
                      { name: "Completed", value: stats.totalCompleted },
                      { name: "No-show", value: stats.totalNoShow },
                      { name: "Cancelled", value: stats.totalCancelled },
                      { name: "Still booked", value: Math.max(stats.totalAppointments - stats.totalCompleted - stats.totalNoShow - stats.totalCancelled, 0) },
                    ]}
                    dataKey="value"
                    nameKey="name"
                    outerRadius={90}
                    label
                  >
                    {OUTCOME_COLORS.map((c, i) => <Cell key={i} fill={c} />)}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
