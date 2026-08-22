import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getActiveDepartments } from '../api/departments';
import { createTicket, getMyTickets, getAssignedTickets, getClaimableTickets, claimTicket, searchTickets } from '../api/tickets';
import Pagination from '../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { TicketStatusBadge, TicketPriorityBadge, UnreadDot } from '../components/TicketBadges';
import { useAuth } from '../context/AuthContext';

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'CLOSED'];
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH'];

const EMPTY_FORM = { departmentId: '', title: '', description: '', priority: 'MEDIUM' };
const EMPTY_FILTERS = { status: '', priority: '' };

function TicketTable({ tickets, showDepartment, onClaim }) {
  if (!tickets?.length) {
    return <p className="empty-row">No tickets found for these filters</p>;
  }
  return (
    <div className="data-table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Ticket #</th>
            <th>Title</th>
            {showDepartment && <th>Department</th>}
            <th>Raised By</th>
            <th>Assigned To</th>
            <th>Priority</th>
            <th>Status</th>
            {onClaim && <th>Action</th>}
          </tr>
        </thead>
        <tbody>
          {tickets.map((t) => (
            <tr key={t.id}>
              <td>
                <Link to={`/tickets/${t.id}`}>{t.ticketNumber}</Link>
                {t.hasUnread && <UnreadDot />}
              </td>
              <td>{t.title}</td>
              {showDepartment && <td>{t.departmentName}</td>}
              <td>{t.raisedByName}</td>
              <td>{t.assignedToName || '—'}</td>
              <td><TicketPriorityBadge priority={t.priority} /></td>
              <td><TicketStatusBadge status={t.status} /></td>
              {onClaim && (
                <td>
                  <button className="btn btn-primary btn-sm" onClick={() => onClaim(t.id)}>Claim</button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FilterSelects({ filters, onChange, showStatus = true }) {
  return (
    <div className="filter-bar">
      {showStatus && (
        <label>
          Status
          <select value={filters.status} onChange={(e) => onChange({ ...filters, status: e.target.value })}>
            <option value="">All</option>
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
          </select>
        </label>
      )}
      <label>
        Priority
        <select value={filters.priority} onChange={(e) => onChange({ ...filters, priority: e.target.value })}>
          <option value="">All</option>
          {PRIORITY_OPTIONS.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
      </label>
    </div>
  );
}

export default function Tickets() {
  const { isStaff } = useAuth();
  const [tab, setTab] = useState('my'); // 'my' | 'assigned' | 'claimable' | 'all'
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);

  const [raising, setRaising] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);

  useEffect(() => {
    getActiveDepartments().then((res) => setDepartments(res.data || [])).catch((err) => setError(err.message));
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    const params = { ...filters, page, size: 10 };
    const call = tab === 'my' ? getMyTickets(params)
      : tab === 'assigned' ? getAssignedTickets(params)
      // Claimable pool is always OPEN + unassigned by definition, so the
      // status filter doesn't apply here - only priority is passed through.
      : tab === 'claimable' ? getClaimableTickets({ priority: filters.priority, page, size: 10 })
      : searchTickets(params);
    call
      .then((res) => setResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [tab, filters, page]);

  useEffect(() => { load(); }, [load]);

  async function handleClaim(ticketId) {
    setError('');
    try {
      await claimTicket(ticketId);
      setSuccess('Ticket claimed');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  function changeTab(next) {
    setTab(next);
    setPage(0);
    setFilters(EMPTY_FILTERS);
  }

  function openRaise() {
    setForm({ ...EMPTY_FORM, departmentId: departments[0]?.id || '' });
    setRaising(true);
  }

  async function handleRaise(e) {
    e.preventDefault();
    setError('');
    try {
      const dto = { ...form, departmentId: Number(form.departmentId) };
      await createTicket(dto);
      setSuccess('Ticket raised');
      setRaising(false);
      if (tab === 'my') load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Tickets</h1>
        <button className="btn btn-primary" onClick={openRaise}>+ Raise Ticket</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="tabs" role="tablist">
        <button type="button" className={`btn ${tab === 'my' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => changeTab('my')}>My Tickets</button>
        <button type="button" className={`btn ${tab === 'assigned' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => changeTab('assigned')}>Assigned to Me</button>
        <button type="button" className={`btn ${tab === 'claimable' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => changeTab('claimable')}>Claimable</button>
        {isStaff && (
          <button type="button" className={`btn ${tab === 'all' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => changeTab('all')}>All Tickets</button>
        )}
      </div>

      <FilterSelects filters={filters} onChange={(f) => { setPage(0); setFilters(f); }} showStatus={tab !== 'claimable'} />

      {loading ? <Loading /> : (
        <>
          <TicketTable tickets={results?.content} showDepartment onClaim={tab === 'claimable' ? handleClaim : undefined} />
          {results && (
            <Pagination
              pageNumber={results.pageNumber}
              totalPages={results.totalPages}
              first={results.first}
              last={results.last}
              onChange={setPage}
            />
          )}
        </>
      )}

      {raising && (
        <div className="modal-backdrop" onClick={() => setRaising(false)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleRaise}>
            <h2>Raise a Ticket</h2>
            <div className="form-grid">
              <label>
                Department
                <select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })} required>
                  <option value="" disabled>Select department</option>
                  {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </label>
              <label>
                Priority
                <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                  {PRIORITY_OPTIONS.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </label>
              <label>
                Title
                <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} maxLength={200} required />
              </label>
              <label>
                Description
                <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} maxLength={2000} rows={5} required />
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setRaising(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Raise Ticket</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
