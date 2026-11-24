import { useEffect, useState, useCallback } from 'react';
import { getDesignations, createDesignation, updateDesignation, deleteDesignation } from '../api/designations';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const emptyForm = { name: '', description: '', active: true };

export default function Designations() {
  const [designations, setDesignations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null); // null closed, {} new, {...} edit
  const [form, setForm] = useState(emptyForm);
  const { hasRole } = useAuth();
  const canWrite = hasRole('ADMIN') || hasRole('MANAGER');

  const load = useCallback(() => {
    setLoading(true);
    getDesignations()
      .then((res) => setDesignations(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm(emptyForm);
    setEditing({});
  }

  function openEdit(designation) {
    setForm({ name: designation.name, description: designation.description || '', active: designation.active });
    setEditing(designation);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateDesignation(editing.id, form);
        setSuccess('Designation updated');
      } else {
        await createDesignation(form);
        setSuccess('Designation created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this designation?')) return;
    try {
      await deleteDesignation(id);
      setSuccess('Designation deleted');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Designations</h1>
        {canWrite && <button className="btn btn-primary" onClick={openNew}>+ New Designation</button>}
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Employees</th>
              <th>Active</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {designations.length ? designations.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.description}</td>
                <td>{d.employeeCount ?? 0}</td>
                <td>{d.active ? 'Yes' : 'No'}</td>
                {canWrite && (
                  <td className="row-actions">
                    <button className="btn btn-link" onClick={() => openEdit(d)}>Edit</button>
                    {hasRole('ADMIN') && (
                      <button className="btn btn-link btn-danger" onClick={() => handleDelete(d.id)}>Delete</button>
                    )}
                  </td>
                )}
              </tr>
            )) : (
              <tr><td colSpan={5} className="empty-row">No designations found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Designation' : 'New Designation'}</h2>
            <div className="form-grid">
              <label className="span-2">
                Name
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required placeholder="e.g. Software Engineer" />
              </label>
              <label className="span-2">
                Description
                <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </label>
              <label className="checkbox-label">
                <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                Active
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-link" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
