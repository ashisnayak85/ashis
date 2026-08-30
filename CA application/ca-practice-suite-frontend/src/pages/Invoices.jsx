import { useEffect, useState, useCallback } from 'react';
import { searchInvoices, createInvoice, updateInvoiceStatus, deleteInvoice } from '../api/invoices';
import { getActiveClients } from '../api/clients';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';

const emptyForm = { clientId: '', invoiceType: 'SALES', invoiceDate: new Date().toISOString().slice(0, 10), dueDate: '', subtotal: '', gstRate: '18', description: '' };
const NEXT_STATUS = { DRAFT: ['SENT', 'CANCELLED'], SENT: ['PAID', 'OVERDUE', 'CANCELLED'], OVERDUE: ['PAID', 'CANCELLED'] };

export default function Invoices() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [clients, setClients] = useState([]);
  const [filters, setFilters] = useState({ clientId: '', type: '', status: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback((page = 0) => {
    setLoading(true);
    const params = { page, size: 10 };
    if (filters.clientId) params.clientId = filters.clientId;
    if (filters.type) params.type = filters.type;
    if (filters.status) params.status = filters.status;
    searchInvoices(params)
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
      await createInvoice({ ...form, clientId: Number(form.clientId), subtotal: Number(form.subtotal), gstRate: Number(form.gstRate || 0) });
      setSuccess('Invoice created as DRAFT');
      setForm(emptyForm);
      setShowForm(false);
      load(0);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleStatus(id, status) {
    try {
      await updateInvoiceStatus(id, status);
      setSuccess(`Invoice marked ${status}`);
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this draft invoice?')) return;
    try {
      await deleteInvoice(id);
      setSuccess('Invoice deleted');
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Invoices</h1>
        <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>+ New Invoice</button>
      </div>

      <div className="toolbar">
        <select value={filters.clientId} onChange={(e) => setFilters({ ...filters, clientId: e.target.value })}>
          <option value="">All clients</option>
          {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value })}>
          <option value="">All types</option>
          <option value="SALES">Sales</option>
          <option value="PURCHASE">Purchase</option>
        </select>
        <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">All statuses</option>
          {['DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {showForm && (
        <form className="inline-panel" onSubmit={handleCreate}>
          <div className="form-grid">
            <label className="form-field">
              <span>Client *</span>
              <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })} required>
                <option value="">Select…</option>
                {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Type *</span>
              <select value={form.invoiceType} onChange={(e) => setForm({ ...form, invoiceType: e.target.value })}>
                <option value="SALES">Sales</option>
                <option value="PURCHASE">Purchase</option>
              </select>
            </label>
            <label className="form-field">
              <span>Invoice Date *</span>
              <input type="date" value={form.invoiceDate} onChange={(e) => setForm({ ...form, invoiceDate: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Due Date</span>
              <input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
            </label>
            <label className="form-field">
              <span>Subtotal *</span>
              <input type="number" min="0.01" step="0.01" value={form.subtotal} onChange={(e) => setForm({ ...form, subtotal: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>GST Rate %</span>
              <input type="number" min="0" step="0.01" value={form.gstRate} onChange={(e) => setForm({ ...form, gstRate: e.target.value })} />
            </label>
            <label className="form-field form-field-wide">
              <span>Description</span>
              <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary">Save as Draft</button>
          </div>
        </form>
      )}

      {loading ? <Loading /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>#</th><th>Client</th><th>Type</th><th>Date</th><th>Total</th><th>Status</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.content.length ? data.content.map((inv) => (
                <tr key={inv.id}>
                  <td>{inv.invoiceNumber}</td>
                  <td>{inv.clientName}</td>
                  <td>{inv.invoiceType}</td>
                  <td>{inv.invoiceDate}</td>
                  <td>₹{Number(inv.totalAmount).toLocaleString('en-IN')}</td>
                  <td><StatusBadge status={inv.status} /></td>
                  <td className="row-actions">
                    {(NEXT_STATUS[inv.status] || []).map((s) => (
                      <button key={s} className="btn btn-link" onClick={() => handleStatus(inv.id, s)}>Mark {s}</button>
                    ))}
                    {inv.status === 'DRAFT' && (
                      <button className="btn btn-link btn-danger" onClick={() => handleDelete(inv.id)}>Delete</button>
                    )}
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={7} className="empty-state">No invoices match these filters</td></tr>
              )}
            </tbody>
          </table>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={load} />
        </>
      )}
    </div>
  );
}
