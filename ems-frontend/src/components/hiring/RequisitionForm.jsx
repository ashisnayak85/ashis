import { useState } from 'react';
import { createRequisition, updateRequisition } from '../../api/hiring';
import { EmployeePicker } from './HiringBits';
import { emptyToNull, numOrNull } from './hiringUtils';
import { ErrorBanner } from '../Feedback';

// Create / edit a requisition. `initial` (a RequisitionDTO) switches it into edit mode.
export default function RequisitionForm({ lookups, initial, onSaved, onCancel }) {
  const editing = !!initial;
  const [form, setForm] = useState({
    title: initial?.title || '',
    departmentId: initial?.departmentId || '',
    designationId: initial?.designationId || '',
    employeeTypeId: initial?.employeeTypeId || '',
    locationId: initial?.locationId || '',
    openings: initial?.openings ?? 1,
    budgetMin: initial?.budgetMin ?? '',
    budgetMax: initial?.budgetMax ?? '',
    minExperience: initial?.minExperience ?? '',
    maxExperience: initial?.maxExperience ?? '',
    targetCloseDate: initial?.targetCloseDate || '',
    jobDescription: initial?.jobDescription || '',
    justification: initial?.justification || '',
  });
  const [manager, setManager] = useState(
    initial ? { id: initial.hiringManagerId, label: initial.hiringManagerName } : null,
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState([]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setErrors([]);
    const body = {
      title: form.title.trim(),
      departmentId: Number(form.departmentId),
      designationId: Number(form.designationId),
      employeeTypeId: Number(form.employeeTypeId),
      locationId: numOrNull(form.locationId),
      openings: Number(form.openings),
      budgetMin: numOrNull(form.budgetMin),
      budgetMax: numOrNull(form.budgetMax),
      minExperience: numOrNull(form.minExperience),
      maxExperience: numOrNull(form.maxExperience),
      targetCloseDate: emptyToNull(form.targetCloseDate),
      jobDescription: emptyToNull(form.jobDescription),
      justification: emptyToNull(form.justification),
      hiringManagerId: manager?.id ?? null, // empty = the person raising it
    };
    try {
      const res = editing ? await updateRequisition(initial.id, body) : await createRequisition(body);
      onSaved(res.data);
    } catch (err) {
      setError(err.message);
      setErrors(err.errors || []);
      setBusy(false);
    }
  }

  const options = (list) => (list || []).map((x) => <option key={x.id} value={x.id}>{x.name}</option>);

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{editing ? 'Edit requisition' : 'New requisition'}</h2>
        <ErrorBanner message={error} errors={errors} />
        <div className="form-grid">
          <label>
            Job title
            <input value={form.title} onChange={set('title')} maxLength={150} required />
          </label>
          <label>
            Openings
            <input type="number" min={1} max={500} value={form.openings} onChange={set('openings')} required />
          </label>
          <label>
            Department
            <select value={form.departmentId} onChange={set('departmentId')} required>
              <option value="" disabled>Select…</option>
              {options(lookups?.departments)}
            </select>
          </label>
          <label>
            Designation
            <select value={form.designationId} onChange={set('designationId')} required>
              <option value="" disabled>Select…</option>
              {options(lookups?.designations)}
            </select>
          </label>
          <label>
            Employee type
            <select value={form.employeeTypeId} onChange={set('employeeTypeId')} required>
              <option value="" disabled>Select…</option>
              {options(lookups?.employeeTypes)}
            </select>
          </label>
          <label>
            Location
            <select value={form.locationId} onChange={set('locationId')}>
              <option value="">— Any / not specified —</option>
              {options(lookups?.locations)}
            </select>
          </label>
          <label>
            Budget min (annual CTC)
            <input type="number" min={0} step="0.01" value={form.budgetMin} onChange={set('budgetMin')} />
          </label>
          <label>
            Budget max (annual CTC)
            <input type="number" min={0} step="0.01" value={form.budgetMax} onChange={set('budgetMax')} />
          </label>
          <label>
            Experience min (years)
            <input type="number" min={0} step="0.5" value={form.minExperience} onChange={set('minExperience')} />
          </label>
          <label>
            Experience max (years)
            <input type="number" min={0} step="0.5" value={form.maxExperience} onChange={set('maxExperience')} />
          </label>
          <label>
            Target close date
            <input type="date" value={form.targetCloseDate} onChange={set('targetCloseDate')} />
          </label>
          <EmployeePicker
            label="Hiring manager (default: you)"
            value={manager}
            onChange={setManager}
          />
        </div>
        <label>
          Job description
          <textarea value={form.jobDescription} onChange={set('jobDescription')} maxLength={4000} rows={4} />
        </label>
        <label>
          Justification (why is this role needed?)
          <textarea value={form.justification} onChange={set('justification')} maxLength={1000} rows={3} />
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </div>
  );
}
