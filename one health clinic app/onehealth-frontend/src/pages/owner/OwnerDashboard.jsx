import { useEffect, useState } from "react";
import DashboardLayout from "../../components/DashboardLayout";
import { getDashboard, getClinics } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  PieChart, Pie, Cell, LineChart, Line,
} from "recharts";

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

const SOURCE_COLORS = ["#3d5488", "#6a3fb0"];
const OUTCOME_COLORS = ["#2f8f5b", "#c0392b", "#8a5a12", "#5b6b73"];

/**
 * The owner's cross-branch analytics view. Defaults from/to to today (per the
 * requirement), and re-fetches whenever the date range or branch filter
 * changes. All charts come straight from GET /api/owner/dashboard.
 */
export default function OwnerDashboard() {
  const [clinics, setClinics] = useState([]);
  const [from, setFrom] = useState(todayStr());
  const [to, setTo] = useState(todayStr());
  const [clinicId, setClinicId] = useState("");
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getClinics().then(setClinics).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    setError("");
    getDashboard({ from, to, clinicId: clinicId || undefined })
      .then(setStats)
      .catch(() => setError("Couldn't load dashboard stats."))
      .finally(() => setLoading(false));
  }, [from, to, clinicId]);

  function applyPreset(preset) {
    const today = new Date();
    if (preset === "today") {
      setFrom(todayStr());
      setTo(todayStr());
    } else if (preset === "7d") {
      const start = new Date(today);
      start.setDate(start.getDate() - 6);
      setFrom(start.toISOString().slice(0, 10));
      setTo(todayStr());
    } else if (preset === "30d") {
      const start = new Date(today);
      start.setDate(start.getDate() - 29);
      setFrom(start.toISOString().slice(0, 10));
      setTo(todayStr());
    }
  }

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
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
        <div className="field">
          <label>Branch</label>
          <select value={clinicId} onChange={(e) => setClinicId(e.target.value)}>
            <option value="">All branches</option>
            {clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName}</option>
            ))}
          </select>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="btn btn-secondary btn-sm" onClick={() => applyPreset("today")}>Today</button>
          <button className="btn btn-secondary btn-sm" onClick={() => applyPreset("7d")}>Last 7 days</button>
          <button className="btn btn-secondary btn-sm" onClick={() => applyPreset("30d")}>Last 30 days</button>
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
            <div className="stat-card">
              <div className="stat-value">₹{Number(stats.totalRevenue).toLocaleString()}</div>
              <div className="stat-label">Revenue (paid)</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.activeClinicCount}</div>
              <div className="stat-label">Active branches</div>
            </div>
          </div>

          <div className="chart-grid">
            <div className="chart-card wide">
              <h3>Location-wise completion rate (% patients served)</h3>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={stats.byClinic}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="clinicName" />
                  <YAxis unit="%" domain={[0, 100]} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="completionRatePercent" name="Completion %" fill="#2f8f5b" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="chart-card">
              <h3>Appointments by branch (volume)</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={stats.byClinic}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="clinicName" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="onlineBookings" name="Online" stackId="a" fill={SOURCE_COLORS[0]} />
                  <Bar dataKey="walkIns" name="Walk-in" stackId="a" fill={SOURCE_COLORS[1]} radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="chart-card">
              <h3>Overall outcome split</h3>
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

            <div className="chart-card wide">
              <h3>Appointments over time</h3>
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={stats.trend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="total" name="Total" stroke="#3d5488" strokeWidth={2} />
                  <Line type="monotone" dataKey="completed" name="Completed" stroke="#2f8f5b" strokeWidth={2} />
                  <Line type="monotone" dataKey="noShow" name="No-show" stroke="#c0392b" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="section-header">
            <h3>Branch breakdown</h3>
          </div>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Branch</th><th>Total</th><th>Online</th><th>Walk-in</th>
                  <th>Completed</th><th>No-show</th><th>Cancelled</th>
                  <th>Completion %</th><th>Revenue</th><th>Patients</th>
                </tr>
              </thead>
              <tbody>
                {stats.byClinic.map((c) => (
                  <tr key={c.clinicId}>
                    <td>{c.clinicName}</td>
                    <td>{c.totalAppointments}</td>
                    <td>{c.onlineBookings}</td>
                    <td>{c.walkIns}</td>
                    <td>{c.completed}</td>
                    <td>{c.noShow}</td>
                    <td>{c.cancelled}</td>
                    <td>{c.completionRatePercent}%</td>
                    <td>₹{Number(c.revenue).toLocaleString()}</td>
                    <td>{c.uniquePatients}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="section-header">
            <h3>Doctor utilization</h3>
          </div>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr><th>Doctor</th><th>Branch</th><th>Slots booked / total</th><th>Utilization</th></tr>
              </thead>
              <tbody>
                {stats.doctorUtilization.length === 0 ? (
                  <tr><td colSpan={4} className="empty-state">No availability generated for this range yet.</td></tr>
                ) : stats.doctorUtilization.map((d) => (
                  <tr key={`${d.doctorId}-${d.clinicId}`}>
                    <td>{d.doctorName}</td>
                    <td>{d.clinicName}</td>
                    <td>{d.slotsBooked} / {d.slotsTotal}</td>
                    <td>{d.utilizationRatePercent}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
