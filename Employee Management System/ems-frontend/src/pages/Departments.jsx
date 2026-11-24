import { useEffect, useState, useCallback } from 'react';
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '../api/departments';
import { getEmployees } from '../api/employees';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';

const emptyForm = { name: '', code: '', description: '', active: true, headOfDepartmentId: '' };

export default function Departments() {
  const [departments, setDepartments] = useState([]);
  const [employees, setEmployees] = useState([]);
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
  useEffect(() => {
    getEmployees({ page: 0, size: 200 }).then((res) => setEmployees(res.data?.content || [])).catch(() => { /* HOD dropdown just won't populate */ });
  }, []);

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
      headOfDepartmentId: dept.headOfDepartmentId || '',
    });
    setEditing(dept);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const dto = { ...form, headOfDepartmentId: form.headOfDepartmentId ? Number(form.headOfDepartmentId) : null };
      if (editing.id) {
        await updateDepartment(editing.id, dto);
        setSuccess('Department updated');
      } else {
        await createDepartment(dto);
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
        <h1>Departments</h1>
        {canWrite && <button className="btn btn-primary" onClick={openNew}>+ New Department</button>}
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {loading ? <Loading /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Description</th>
              <th>Employees</th>
              <th>Head of Department</th>
              <th>Active</th>
              {canWrite && <th></th>}
            </tr>
          </thead>
          <tbody>
            {departments.length ? departments.map((d) => (
              <tr key={d.id}>
                <td>{d.code}</td>
                <td>{d.name}</td>
                <td>{d.description}</td>
                <td>{d.employeeCount ?? 0}</td>
                <td>{d.headOfDepartmentName || '—'}</td>
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
              <label>
                Head of Department
                <select value={form.headOfDepartmentId} onChange={(e) => setForm({ ...form, headOfDepartmentId: e.target.value })}>
                  <option value="">None set</option>
                  {employees.map((e) => <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.employeeCode})</option>)}
                </select>
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
