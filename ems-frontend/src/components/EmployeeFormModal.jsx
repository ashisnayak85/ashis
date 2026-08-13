import { useEffect, useState } from 'react';
import { getDepartments } from '../api/departments';
import { ErrorBanner } from './Feedback';

const emptyForm = {
  employeeCode: '',
  firstName: '',
  lastName: '',
  email: '',
  mobile: '',
  dateOfBirth: '',
  dateOfJoining: '',
  salary: '',
  designation: '',
  departmentId: '',
  active: true,
};

export default function EmployeeFormModal({ employee, onClose, onSubmit }) {
  const [form, setForm] = useState(employee ? { ...emptyForm, ...employee } : emptyForm);
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getDepartments().then((res) => setDepartments(res.data || []));
  }, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await onSubmit({
        ...form,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
        salary: form.salary === '' ? null : Number(form.salary),
      });
    } catch (err) {
      setError(err.message || 'Failed to save employee');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{employee ? 'Edit Employee' : 'New Employee'}</h2>
        <ErrorBanner message={error} />

        <div className="form-grid">
          <label>
            Employee Code
            <input value={form.employeeCode} onChange={(e) => update('employeeCode', e.target.value.toUpperCase())} required disabled={!!employee} />
          </label>
          <label>
            First Name
            <input value={form.firstName} onChange={(e) => update('firstName', e.target.value)} required />
          </label>
          <label>
            Last Name
            <input value={form.lastName} onChange={(e) => update('lastName', e.target.value)} required />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)} required />
          </label>
          <label>
            Mobile
            <input value={form.mobile || ''} onChange={(e) => update('mobile', e.target.value)} placeholder="10-digit Indian mobile" />
          </label>
          <label>
            Date of Birth
            <input type="date" value={form.dateOfBirth || ''} onChange={(e) => update('dateOfBirth', e.target.value)} />
          </label>
          <label>
            Date of Joining
            <input type="date" value={form.dateOfJoining || ''} onChange={(e) => update('dateOfJoining', e.target.value)} required />
          </label>
          <label>
            Salary
            <input type="number" min="0" value={form.salary ?? ''} onChange={(e) => update('salary', e.target.value)} />
          </label>
          <label>
            Designation
            <input value={form.designation || ''} onChange={(e) => update('designation', e.target.value)} />
          </label>
          <label>
            Department
            <select value={form.departmentId || ''} onChange={(e) => update('departmentId', e.target.value)} required>
              <option value="" disabled>Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </label>
          <label className="checkbox-label">
            <input type="checkbox" checked={!!form.active} onChange={(e) => update('active', e.target.checked)} />
            Active
          </label>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}
