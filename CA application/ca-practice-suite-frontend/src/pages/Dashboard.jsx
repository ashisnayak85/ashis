import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getStats } from '../api/dashboard';
import { Loading, ErrorBanner } from '../components/Feedback';
import StatusBadge from '../components/StatusBadge';

function money(n) {
  return `₹${Number(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

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
      <div className="page-header">
        <h1>Dashboard</h1>
      </div>
      <ErrorBanner message={error} />

      {stats && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-label">Active Clients</div>
              <div className="stat-value">{stats.activeClients} <span className="stat-value-sub">/ {stats.totalClients}</span></div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Income (FY to date)</div>
              <div className="stat-value">{money(stats.totalIncome)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Expense (FY to date)</div>
              <div className="stat-value">{money(stats.totalExpense)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Net Position</div>
              <div className={`stat-value ${Number(stats.netPosition) < 0 ? 'stat-negative' : ''}`}>{money(stats.netPosition)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">GST Payable (Output − Input)</div>
              <div className="stat-value">{money(stats.gstPayable)}</div>
              <div className="stat-hint">Collected {money(stats.gstCollected)} · Input credit {money(stats.gstInputCredit)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Unpaid Invoices</div>
              <div className="stat-value">{stats.unpaidInvoices}</div>
              <div className="stat-hint">{money(stats.unpaidInvoiceTotal)} outstanding</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Pending Compliance</div>
              <div className="stat-value">{stats.pendingComplianceTasks}</div>
            </div>
            <div className="stat-card stat-card-danger">
              <div className="stat-label">Overdue Compliance</div>
              <div className="stat-value">{stats.overdueComplianceTasks}</div>
            </div>
          </div>

          <div className="panel-grid">
            <div className="panel">
              <div className="panel-header">
                <h2>Upcoming Deadlines</h2>
                <Link to="/compliance" className="btn btn-link">View all</Link>
              </div>
              {stats.upcomingDeadlines?.length ? (
                <table className="data-table">
                  <thead>
                    <tr><th>Due</th><th>Task</th><th>Client</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {stats.upcomingDeadlines.map((t) => (
                      <tr key={t.id}>
                        <td>{t.dueDate}</td>
                        <td>{t.title}</td>
                        <td>{t.clientName || '—'}</td>
                        <td><StatusBadge status={t.status} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : <p className="empty-state">Nothing due — the calendar is clear.</p>}
            </div>

            <div className="panel">
              <div className="panel-header">
                <h2>Recent Ledger Activity</h2>
                <Link to="/ledger" className="btn btn-link">View all</Link>
              </div>
              {stats.recentEntries?.length ? (
                <table className="data-table">
                  <thead>
                    <tr><th>Date</th><th>Head</th><th>Client</th><th>Total</th></tr>
                  </thead>
                  <tbody>
                    {stats.recentEntries.map((e) => (
                      <tr key={e.id}>
                        <td>{e.entryDate}</td>
                        <td>{e.accountName}</td>
                        <td>{e.clientName || '—'}</td>
                        <td>{money(e.totalAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : <p className="empty-state">No entries posted yet.</p>}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
