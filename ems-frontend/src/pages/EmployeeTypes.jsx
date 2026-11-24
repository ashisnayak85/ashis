
import { useEffect, useState, useCallback } from 'react';
import { getEmployeeTypes, createEmployeeType, updateEmployeeType, deleteEmployeeType } from '../api/employeeType';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const emptyForm = { name: '', code: '', description: '', active: true };

export default function EmployeeTypes() {
  const [employeeType, setEmployeeType] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null); // null closed, {} new, {...} edit
  const [form, setForm] = useState(emptyForm);
  const { hasRole } = useAuth();
  const canWrite = hasRole('ADMIN') || hasRole('MANAGER');

  const load = useCallback(() => {
    setLoading(true);
    getEmployeeTypes()
      .then((res) => setEmployeeType(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm(emptyForm);
    setEditing({});
  }

  function openEdit(dept) {
    setForm({
      name: dept.name,
      code: dept.code,
      description: dept.description || '',
      active: dept.active,
    });
    setEditing(dept);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const dto = { ...form};
      if (editing.id) {
        await updateEmployeeType(editing.id, dto);
        setSuccess('Department updated');
      } else {
        await createEmployeeType(dto);
        setSuccess('Department created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this Employee Type?')) return;
    try {
      await deleteEmployeeType(id);
      setSuccess('Employee Type deleted');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Employee Type</h1>
        {canWrite && <button className="btn btn-primary" onClick={openNew}>+ New EmployeeType</button>}
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Employee Type</th>
              <th>Description</th>
              <th>Created By</th>
              <th>When Created</th>
              <th>Active</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {employeeType.length ? employeeType.map((d) => (
              <tr key={d.id}>
                <td>{d.code}</td>
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
              <tr><td colSpan={7} className="empty-row">No departments found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Employee Type' : 'New Employee Type'}</h2>
            <div className="form-grid">
              <label>
                Employee Type Name
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label>
                Code
                <input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} required />
              </label>
              <label>
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
