import { useEffect, useState, useCallback } from 'react';
import { getAccounts, createAccount, updateAccount, deleteAccount } from '../api/accounts';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';

const ACCOUNT_TYPES = ['INCOME', 'EXPENSE', 'ASSET', 'LIABILITY', 'EQUITY'];
const emptyForm = { name: '', code: '', accountType: 'EXPENSE', active: true };

export default function Accounts() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(() => {
    setLoading(true);
    getAccounts()
      .then((res) => setAccounts(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm(emptyForm);
    setEditing({});
  }

  function openEdit(a) {
    setForm({ name: a.name, code: a.code, accountType: a.accountType, active: a.active });
    setEditing(a);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateAccount(editing.id, form);
        setSuccess('Ledger head updated');
      } else {
        await createAccount(form);
        setSuccess('Ledger head created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Deactivate this ledger head?')) return;
    try {
      await deleteAccount(id);
      setSuccess('Ledger head deactivated');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Chart of Accounts</h1>
        <button className="btn btn-primary" onClick={openNew}>+ New Ledger Head</button>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr><th>Code</th><th>Name</th><th>Type</th><th>Active</th><th></th></tr>
          </thead>
          <tbody>
            {accounts.length ? accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.code}</td>
                <td>{a.name}</td>
                <td>{a.accountType}</td>
                <td>{a.active ? 'Yes' : 'No'}</td>
                <td className="row-actions">
                  <button className="btn btn-link" onClick={() => openEdit(a)}>Edit</button>
                  <button className="btn btn-link btn-danger" onClick={() => handleDelete(a.id)}>Deactivate</button>
                </td>
              </tr>
            )) : (
              <tr><td colSpan={5} className="empty-state">No ledger heads yet</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Ledger Head' : 'New Ledger Head'}</h2>
            <div className="form-grid">
              <label className="form-field">
                <span>Name *</span>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label className="form-field">
                <span>Code *</span>
                <input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} disabled={!!editing.id} required />
              </label>
              <label className="form-field">
                <span>Account Type *</span>
                <select value={form.accountType} onChange={(e) => setForm({ ...form, accountType: e.target.value })}>
                  {ACCOUNT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
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
              <button type="submit" className="btn btn-primary">{editing.id ? 'Save' : 'Create'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
