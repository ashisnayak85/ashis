import { useEffect, useState, useCallback } from 'react';
import { searchClients, createClient, updateClient, deactivateClient } from '../api/clients';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import Pagination from '../components/Pagination';
import { useAuth } from '../context/AuthContext';

const CLIENT_TYPES = ['INDIVIDUAL', 'PROPRIETORSHIP', 'PARTNERSHIP', 'LLP', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'TRUST'];

const emptyForm = {
  name: '', clientType: 'PROPRIETORSHIP', gstin: '', pan: '', email: '', phone: '',
  addressLine: '', city: '', state: '', pincode: '', active: true,
};

export default function Clients() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [nameFilter, setNameFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const { hasRole } = useAuth();

  const load = useCallback((page = 0, name = nameFilter) => {
    setLoading(true);
    searchClients({ page, size: 10, name: name || undefined })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [nameFilter]);

  useEffect(() => { load(0); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function openNew() {
    setForm(emptyForm);
    setEditing({});
  }

  function openEdit(client) {
    setForm({
      name: client.name, clientType: client.clientType, gstin: client.gstin || '', pan: client.pan || '',
      email: client.email || '', phone: client.phone || '', addressLine: client.addressLine || '',
      city: client.city || '', state: client.state || '', pincode: client.pincode || '', active: client.active,
    });
    setEditing(client);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateClient(editing.id, form);
        setSuccess('Client updated');
      } else {
        await createClient(form);
        setSuccess('Client onboarded');
      }
      setEditing(null);
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDeactivate(id) {
    if (!confirm('Deactivate this client?')) return;
    try {
      await deactivateClient(id);
      setSuccess('Client deactivated');
      load(data.page);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Clients</h1>
        <button className="btn btn-primary" onClick={openNew}>+ New Client</button>
      </div>

      <div className="toolbar">
        <input
          placeholder="Search by name…"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && load(0)}
        />
        <button className="btn btn-secondary" onClick={() => load(0)}>Search</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th><th>Type</th><th>GSTIN</th><th>City</th>
                <th>YTD Income</th><th>YTD Expense</th><th>Active</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.content.length ? data.content.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.clientType.replace('_', ' ')}</td>
                  <td>{c.gstin || '—'}</td>
                  <td>{c.city || '—'}</td>
                  <td>₹{Number(c.totalIncome ?? 0).toLocaleString('en-IN')}</td>
                  <td>₹{Number(c.totalExpense ?? 0).toLocaleString('en-IN')}</td>
                  <td>{c.active ? 'Yes' : 'No'}</td>
                  <td className="row-actions">
                    <button className="btn btn-link" onClick={() => openEdit(c)}>Edit</button>
                    {hasRole('ADMIN') && c.active && (
                      <button className="btn btn-link btn-danger" onClick={() => handleDeactivate(c.id)}>Deactivate</button>
                    )}
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={8} className="empty-state">No clients found</td></tr>
              )}
            </tbody>
          </table>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={load} />
        </>
      )}

      {editing && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Client' : 'New Client'}</h2>
            <div className="form-grid">
              <label className="form-field">
                <span>Name *</span>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label className="form-field">
                <span>Client Type *</span>
                <select value={form.clientType} onChange={(e) => setForm({ ...form, clientType: e.target.value })}>
                  {CLIENT_TYPES.map((t) => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
                </select>
              </label>
              <label className="form-field">
                <span>GSTIN</span>
                <input value={form.gstin} onChange={(e) => setForm({ ...form, gstin: e.target.value.toUpperCase() })} placeholder="15-character GSTIN" />
              </label>
              <label className="form-field">
                <span>PAN</span>
                <input value={form.pan} onChange={(e) => setForm({ ...form, pan: e.target.value.toUpperCase() })} placeholder="10-character PAN" />
              </label>
              <label className="form-field">
                <span>Email</span>
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </label>
              <label className="form-field">
                <span>Phone</span>
                <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </label>
              <label className="form-field form-field-wide">
                <span>Address</span>
                <input value={form.addressLine} onChange={(e) => setForm({ ...form, addressLine: e.target.value })} />
              </label>
              <label className="form-field">
                <span>City</span>
                <input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
              </label>
              <label className="form-field">
                <span>State</span>
                <input value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} />
              </label>
              <label className="form-field">
                <span>Pincode</span>
                <input value={form.pincode} onChange={(e) => setForm({ ...form, pincode: e.target.value })} />
              </label>
              {editing.id && (
                <label className="form-field form-checkbox">
                  <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                  <span>Active</span>
                </label>
              )}
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">{editing.id ? 'Save' : 'Onboard Client'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
