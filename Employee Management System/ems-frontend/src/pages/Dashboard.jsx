import { useEffect, useState } from 'react';
import { getStats, getMyStats } from '../api/dashboard';
import { Loading, ErrorBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

export default function Dashboard() {
  const { isStaff } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const request = isStaff ? getStats() : getMyStats();
    request
      .then((res) => setStats(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [isStaff]);

  if (loading) return <Loading />;

  return (
    <div>
      <h1>Dashboard</h1>
      <ErrorBanner message={error} />

      {stats && isStaff && (
        <div className="stat-grid">
          <div className="stat-card">
            <div className="stat-label">Total Employees</div>
            <div className="stat-value">{stats.totalEmployees}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Total Departments</div>
            <div className="stat-value">{stats.totalDepartments}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Present Today</div>
            <div className="stat-value">{stats.presentToday}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Pending Leaves</div>
            <div className="stat-value">{stats.pendingLeaves}</div>
          </div>
        </div>
      )}

      {stats && !isStaff && (
        <>
          <p className="page-subtitle">Your summary for this month, {stats.employeeName}.</p>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-label">Present Days</div>
              <div className="stat-value">{stats.presentDaysThisMonth}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Absent Days</div>
              <div className="stat-value">{stats.absentDaysThisMonth}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Half Days</div>
              <div className="stat-value">{stats.halfDaysThisMonth}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">On Leave Days</div>
              <div className="stat-value">{stats.onLeaveDaysThisMonth}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Pending Leaves</div>
              <div className="stat-value">{stats.pendingLeaves}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Approved Leaves</div>
              <div className="stat-value">{stats.approvedLeaves}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Rejected Leaves</div>
              <div className="stat-value">{stats.rejectedLeaves}</div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
