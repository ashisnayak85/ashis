import { useEffect, useState } from 'react';
import { createOffer, updateOffer, getHiringLookups } from '../../api/hiring';
import { emptyToNull, money } from './hiringUtils';
import { ErrorBanner } from '../Feedback';

// Draft (applicationId) or edit (initial = OfferDTO) an offer. `requisition` supplies the budget band + default designation.
export default function OfferForm({ applicationId, initial, requisition, lookups, onSaved, onCancel }) {
  const editing = !!initial;
  const [form, setForm] = useState({
    designationId: initial?.designationId || requisition?.designationId || '',
    annualCtc: initial?.annualCtc ?? '',
    joiningDate: initial?.joiningDate || '',
    expiryDate: initial?.expiryDate || '',
    budgetExceptionReason: initial?.budgetExceptionReason || '',
    remarks: initial?.remarks || '',
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState([]);

  // The designation list. Normally handed in by the parent page, but the form does not depend on that:
  // if it is missing or empty it loads the list itself, and says so on screen if that fails.
  const [designations, setDesignations] = useState(lookups?.designations || []);
  const [loadingList, setLoadingList] = useState(!(lookups?.designations?.length));
  const [listError, setListError] = useState('');

  useEffect(() => {
    if (designations.length > 0) return undefined;
    let cancelled = false;
    getHiringLookups()
      .then((res) => { if (!cancelled) setDesignations(res.data?.designations || []); })
      .catch((err) => { if (!cancelled) setListError(err.message); })
      .finally(() => { if (!cancelled) setLoadingList(false); });
    return () => { cancelled = true; };
  }, [designations.length]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const budgetMax = requisition?.budgetMax ?? initial?.requisitionBudgetMax ?? null;
  const budgetMin = requisition?.budgetMin ?? initial?.requisitionBudgetMin ?? null;
  const above = budgetMax !== null && form.annualCtc !== '' && Number(form.annualCtc) > Number(budgetMax);

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setErrors([]);
    const body = {
      designationId: Number(form.designationId),
      annualCtc: Number(form.annualCtc),
      joiningDate: form.joiningDate,
      expiryDate: form.expiryDate,
      budgetExceptionReason: above ? emptyToNull(form.budgetExceptionReason.trim()) : null,
      remarks: emptyToNull(form.remarks.trim()),
    };
    try {
      const res = editing ? await updateOffer(initial.id, body) : await createOffer(applicationId, body);
      onSaved(res.data);
    } catch (err) {
      setError(err.message);
      setErrors(err.errors || []);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{editing ? 'Edit offer' : 'Draft offer'}</h2>
        <ErrorBanner message={error} errors={errors} />
        {(budgetMin !== null || budgetMax !== null) && (
          <p className="hint-text">Requisition budget (annual CTC): {money(budgetMin)} – {money(budgetMax)}</p>
        )}
        {listError && (
          <div className="banner banner-error">Could not load the designation list: {listError}</div>
        )}
        {!loadingList && !listError && designations.length === 0 && (
          <div className="banner banner-error">
            No active designations exist yet. Add one under Designations (master data), then reopen this form.
          </div>
        )}
        <div className="form-grid">
          <label>
            Designation
            <select value={form.designationId} onChange={set('designationId')} required disabled={loadingList}>
              <option value="" disabled>{loadingList ? 'Loading designations…' : 'Select…'}</option>
              {designations.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </label>
          <label>
            Annual CTC
            <input type="number" min={1} step="0.01" value={form.annualCtc} onChange={set('annualCtc')} required />
          </label>
          <label>
            Joining date
            <input type="date" value={form.joiningDate} onChange={set('joiningDate')} required />
          </label>
          <label>
            Offer valid until
            <input type="date" value={form.expiryDate} onChange={set('expiryDate')} required />
          </label>
        </div>
        {above && (
          <label>
            This is above the requisition&apos;s maximum budget — reason for the exception (required)
            <textarea value={form.budgetExceptionReason} onChange={set('budgetExceptionReason')} maxLength={500} required />
          </label>
        )}
        <label>
          Remarks (printed on the offer letter)
          <textarea value={form.remarks} onChange={set('remarks')} maxLength={500} rows={2} />
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save draft'}</button>
        </div>
      </form>
    </div>
  );
}