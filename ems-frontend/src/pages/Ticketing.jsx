import { useEffect, useState, useCallback } from 'react';
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '../api/departments';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const emptyForm = { name: '', code: '', description: '', active: true };

export default function Ticketing() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(null); // null closed, {} new, {...} edit
  const [form, setForm] = useState(emptyForm);
  const { hasRole } = useAuth();
  const canWrite = hasRole('ADMIN') || hasRole('MANAGER');

  const load = useCallback(() => {
    setLoading(true);
    getDepartments()
      .then((res) => setDepartments(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  function openNew() {
    setForm(emptyForm);
    setEditing({});
  }

  function openEdit(dept) {
    setForm({ name: dept.name, code: dept.code, description: dept.description || '', active: dept.active });
    setEditing(dept);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) {
        await updateDepartment(editing.id, form);
        setSuccess('Department updated');
      } else {
        await createDepartment(form);
        setSuccess('Department created');
      }
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this department?')) return;
    try {
      await deleteDepartment(id);
      setSuccess('Department deleted');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Ticketing</h1>
        {canWrite && <button className="btn btn-primary" onClick={openNew}>+ New Department</button>}
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Department</th>
              <th>Deaprtment Head</th>
              {/* <th>Description</th>
              <th>Employees</th>
              <th>Active</th> */}
              {canWrite && <th>Action</th>}
            </tr>
          </thead>
          <tbody>
            {departments.length ? departments.map((d) => (
              <tr key={d.id}>
                <td>{d.code}</td>
                <td>{d.name}</td>
                <td>demo</td>
                {/* <td>{d.description}</td>
                <td>{d.employeeCount ?? 0}</td>
                <td>{d.active ? 'Yes' : 'No'}</td> */}
                {canWrite && (
                  <td className="row-actions">
                    <button className="btn btn-link btn-success" onClick={() => openEdit(d)}>Resposible Person</button>
                    {/* {hasRole('ADMIN') && (
                      <button className="btn btn-link btn-danger" onClick={() => handleDelete(d.id)}>Delete</button>
                    )} */}
                  </td>
                )}
              </tr>
            )) : (
              <tr><td colSpan={6} className="empty-row">No departments found</td></tr>
            )}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h2>{editing.id ? 'Edit Department' : 'New Department'}</h2>
            <div className="form-grid">
              <label>
                Name
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
