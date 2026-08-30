import { useEffect, useState, useCallback } from 'react';
import { searchLedger, postEntry, toggleReconciled, deleteEntry, exportLedger } from '../api/ledger';
import { getAccounts } from '../api/accounts';
import { getActiveClients } from '../api/clients';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import Pagination from '../components/Pagination';

const emptyForm = { clientId: '', accountId: '', entryType: 'DEBIT', entryDate: new Date().toISOString().slice(0, 10), amount: '', gstRate: '0', description: '', referenceNumber: '' };

export default function Ledger() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [accounts, setAccounts] = useState([]);
  const [clients, setClients] = useState([]);
  const [filters, setFilters] = useState({ clientId: '', accountType: '', start: '', end: '', reconciled: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback((page = 0) => {
    setLoading(true);
    const params = { page, size: 10 };
    if (filters.clientId) params.clientId = filters.clientId;
    if (filters.accountType) params.accountType = filters.accountType;
    if (filters.start) params.start = filters.start;
    if (filters.end) params.end = filters.end;
    if (filters.reconciled !== '') params.reconciled = filters.reconciled;
    searchLedger(params)
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => {
    getAccounts().then((res) => setAccounts(res.data || [])).catch(() => {});
    getActiveClients().then((res) => setClients(res.data || [])).catch(() => {});
  }, []);
  useEffect(() => { load(0); }, [filters]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handlePost(e) {
    e.preventDefault();
    setError('');
    try {
      await postEntry({
        ...form,
        clientId: form.clientId || null,
        accountId: Number(form.accountId),
        amount: Number(form.amount),
        gstRate: Number(form.gstRate || 0),
      });
      setSuccess('Entry posted');
      setForm(emptyForm);
      setShowForm(false);
      load(0);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleToggle(id) {
    try {
      await toggleReconciled(id);
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this ledger entry?')) return;
    try {
      await deleteEntry(id);
      setSuccess('Entry deleted');
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleExport() {
    try {
      const params = {};
      if (filters.clientId) params.clientId = filters.clientId;
      if (filters.accountType) params.accountType = filters.accountType;
      if (filters.start) params.start = filters.start;
      if (filters.end) params.end = filters.end;
      if (filters.reconciled !== '') params.reconciled = filters.reconciled;
      await exportLedger(params);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Ledger</h1>
        <div className="header-actions">
          <button className="btn btn-secondary" onClick={handleExport}>Export .xlsx</button>
          <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>+ Post Entry</button>
        </div>
      </div>

      <div className="toolbar">
        <select value={filters.clientId} onChange={(e) => setFilters({ ...filters, clientId: e.target.value })}>
          <option value="">All clients</option>
          {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={filters.accountType} onChange={(e) => setFilters({ ...filters, accountType: e.target.value })}>
          <option value="">All types</option>
          <option value="INCOME">Income</option>
          <option value="EXPENSE">Expense</option>
          <option value="ASSET">Asset</option>
          <option value="LIABILITY">Liability</option>
          <option value="EQUITY">Equity</option>
        </select>
        <input type="date" value={filters.start} onChange={(e) => setFilters({ ...filters, start: e.target.value })} />
        <input type="date" value={filters.end} onChange={(e) => setFilters({ ...filters, end: e.target.value })} />
        <select value={filters.reconciled} onChange={(e) => setFilters({ ...filters, reconciled: e.target.value })}>
          <option value="">All</option>
          <option value="true">Reconciled</option>
          <option value="false">Unreconciled</option>
        </select>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {showForm && (
        <form className="inline-panel" onSubmit={handlePost}>
          <div className="form-grid">
            <label className="form-field">
              <span>Client (optional)</span>
              <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
                <option value="">— firm-level —</option>
                {clients.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Ledger Head *</span>
              <select value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })} required>
                <option value="">Select…</option>
                {accounts.map((a) => <option key={a.id} value={a.id}>{a.name} ({a.accountType})</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Entry Type *</span>
              <select value={form.entryType} onChange={(e) => setForm({ ...form, entryType: e.target.value })}>
                <option value="DEBIT">Debit</option>
                <option value="CREDIT">Credit</option>
              </select>
            </label>
            <label className="form-field">
              <span>Date *</span>
              <input type="date" value={form.entryDate} onChange={(e) => setForm({ ...form, entryDate: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>Amount *</span>
              <input type="number" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required />
            </label>
            <label className="form-field">
              <span>GST Rate %</span>
              <input type="number" min="0" step="0.01" value={form.gstRate} onChange={(e) => setForm({ ...form, gstRate: e.target.value })} />
            </label>
            <label className="form-field">
              <span>Reference #</span>
              <input value={form.referenceNumber} onChange={(e) => setForm({ ...form, referenceNumber: e.target.value })} />
            </label>
            <label className="form-field form-field-wide">
              <span>Description</span>
              <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary">Post Entry</button>
          </div>
        </form>
      )}

      {loading ? <Loading /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th><th>Client</th><th>Head</th><th>Type</th><th>Amount</th><th>GST</th><th>Total</th><th>Reconciled</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.content.length ? data.content.map((e) => (
                <tr key={e.id}>
                  <td>{e.entryDate}</td>
                  <td>{e.clientName || '—'}</td>
                  <td>{e.accountName}</td>
                  <td>{e.entryType}</td>
                  <td>₹{Number(e.amount).toLocaleString('en-IN')}</td>
                  <td>₹{Number(e.gstAmount ?? 0).toLocaleString('en-IN')}</td>
                  <td>₹{Number(e.totalAmount).toLocaleString('en-IN')}</td>
                  <td>
                    <button className={`chip ${e.reconciled ? 'chip-success' : 'chip-muted'}`} onClick={() => handleToggle(e.id)}>
                      {e.reconciled ? 'Reconciled' : 'Mark reconciled'}
                    </button>
                  </td>
                  <td className="row-actions">
                    <button className="btn btn-link btn-danger" onClick={() => handleDelete(e.id)}>Delete</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={9} className="empty-state">No ledger entries match these filters</td></tr>
              )}
            </tbody>
          </table>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={load} />
        </>
      )}
    </div>
  );
}
