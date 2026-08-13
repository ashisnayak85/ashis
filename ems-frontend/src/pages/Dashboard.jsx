import { useEffect, useState } from 'react';
import { getStats } from '../api/dashboard';
import { Loading, ErrorBanner } from '../components/Feedback';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStats()
      .then((res) => setStats(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loading />;

  return (
    <div>
      <h1>Dashboard</h1>
      <ErrorBanner message={error} />
      {stats && (
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
    </div>
  );
}
