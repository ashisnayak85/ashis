import { useEffect, useState, useCallback } from 'react';
import { searchCompliance, createTask, markFiled, deleteTask } from '../api/compliance';
import { getActiveClients } from '../api/clients';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';

const TASK_TYPES = ['GSTR1', 'GSTR3B', 'GST_ANNUAL_RETURN', 'TDS_RETURN_24Q', 'TDS_RETURN_26Q', 'ADVANCE_TAX', 'INCOME_TAX_RETURN', 'TAX_AUDIT', 'ROC_ANNUAL_FILING', 'PF_ESI_RETURN', 'PROFESSIONAL_TAX', 'CUSTOM'];
const FREQUENCIES = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUALLY', 'ONE_TIME'];

const emptyForm = { title: '', taskType: 'GSTR3B', clientId: '', frequency: 'MONTHLY', dueDate: '', assignedTo: '', remarks: '' };

export default function Compliance() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [clients, setClients] = useState([]);
  const [filters, setFilters] = useState({ clientId: '', status: '', taskType: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback((page = 0) => {
    setLoading(true);
    const params = { page, size: 10 };
    if (filters.clientId) params.clientId = filters.clientId;
    if (filters.status) params.status = filters.status;
    if (filters.taskType) params.taskType = filters.taskType;
    searchCompliance(params)
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { getActiveClients().then((res) => setClients(res.data || [])).catch(() => {}); }, []);
  useEffect(() => { load(0); }, [filters]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    try {
      await createTask({ ...form, clientId: form.clientId || null });
      setSuccess('Compliance task scheduled');
      setForm(emptyForm);
      setShowForm(false);
      load(0);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleFile(id) {
    try {
      await markFiled(id);
      setSuccess('Marked as filed — next recurrence scheduled automatically');
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this compliance task?')) return;
    try {
      await deleteTask(id);
      setSuccess('Task deleted');
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Compliance Calendar</h1>
        <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>+ Schedule Task</button>
      </div>

      <div className="toolbar">
        <select value={filters.clientId} onChange={(e) => setFilters({ ...filters, clientId: e.target.value })}>
          <option value="">All clients</option>
          {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">All statuses</option>
          {['PENDING', 'IN_PROGRESS', 'FILED', 'OVERDUE'].map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={filters.taskType} onChange={(e) => setFilters({ ...filters, taskType: e.target.value })}>
          <option value="">All task types</option>
          {TASK_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
        </select>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {showForm && (
        <form className="inline-panel" onSubmit={handleCreate}>
          <div className="form-grid">
            <label className="form-field form-field-wide">
              <span>Title *</span>
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. GSTR-3B - August" required />
            </label>
            <label className="form-field">
              <span>Task Type *</span>
              <select value={form.taskType} onChange={(e) => setForm({ ...form, taskType: e.target.value })}>
                {TASK_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Client</span>
              <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
                <option value="">— firm-wide —</option>
                {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Frequency *</span>
              <select value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })}>
                {FREQUENCIES.map((f) => <option key={f} value={f}>{f.replace('_', ' ')}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Due Date *</span>
              <input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Assigned To</span>
              <input value={form.assignedTo} onChange={(e) => setForm({ ...form, assignedTo: e.target.value })} placeholder="username" />
            </label>
            <label className="form-field form-field-wide">
              <span>Remarks</span>
              <input value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
            </label>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary">Schedule</button>
          </div>
        </form>
      )}

      {loading ? <Loading /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Due</th><th>Title</th><th>Type</th><th>Client</th><th>Frequency</th><th>Status</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.content.length ? data.content.map((t) => (
                <tr key={t.id}>
                  <td>{t.dueDate}</td>
                  <td>{t.title}</td>
                  <td>{t.taskType.replace(/_/g, ' ')}</td>
                  <td>{t.clientName || '—'}</td>
                  <td>{t.frequency.replace('_', ' ')}</td>
                  <td><StatusBadge status={t.status} /></td>
                  <td className="row-actions">
                    {t.status !== 'FILED' && (
                      <button className="btn btn-link" onClick={() => handleFile(t.id)}>Mark Filed</button>
                    )}
                    <button className="btn btn-link btn-danger" onClick={() => handleDelete(t.id)}>Delete</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={7} className="empty-state">No compliance tasks match these filters</td></tr>
              )}
            </tbody>
          </table>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={load} />
        </>
      )}
    </div>
  );
}
