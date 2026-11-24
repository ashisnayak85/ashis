import { useState } from 'react';
import { createCandidate, updateCandidate, uploadResume } from '../../api/hiring';
import { EmployeePicker } from './HiringBits';
import { CANDIDATE_SOURCES, emptyToNull, numOrNull, pretty } from './hiringUtils';
import { ErrorBanner } from '../Feedback';

// Create / edit a candidate. `initial` (a CandidateDTO) switches it into edit mode.
export default function CandidateForm({ initial, onSaved, onCancel }) {
  const editing = !!initial;
  const [form, setForm] = useState({
    firstName: initial?.firstName || '',
    lastName: initial?.lastName || '',
    email: initial?.email || '',
    mobile: initial?.mobile || '',
    currentCompany: initial?.currentCompany || '',
    currentDesignation: initial?.currentDesignation || '',
    currentLocation: initial?.currentLocation || '',
    totalExperience: initial?.totalExperience ?? '',
    currentCtc: initial?.currentCtc ?? '',
    expectedCtc: initial?.expectedCtc ?? '',
    noticePeriodDays: initial?.noticePeriodDays ?? '',
    skills: initial?.skills || '',
    source: initial?.source || 'OTHER',
    vendorName: initial?.vendorName || '',
  });
  const [referrer, setReferrer] = useState(
    initial?.referredById ? { id: initial.referredById, label: initial.referredByName } : null,
  );
  const [consent, setConsent] = useState(false);
  const [resumeFile, setResumeFile] = useState(null);
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
      firstName: form.firstName.trim(),
      lastName: emptyToNull(form.lastName.trim()),
      email: form.email.trim(),
      mobile: emptyToNull(form.mobile.trim()),
      currentCompany: emptyToNull(form.currentCompany),
      currentDesignation: emptyToNull(form.currentDesignation),
      currentLocation: emptyToNull(form.currentLocation),
      totalExperience: numOrNull(form.totalExperience),
      currentCtc: numOrNull(form.currentCtc),
      expectedCtc: numOrNull(form.expectedCtc),
      noticePeriodDays: numOrNull(form.noticePeriodDays),
      skills: emptyToNull(form.skills),
      source: form.source,
      vendorName: form.source === 'AGENCY' ? emptyToNull(form.vendorName) : null,
      referredById: form.source === 'REFERRAL' ? referrer?.id ?? null : null,
      consentGiven: editing ? undefined : consent,
    };
    try {
      const res = editing ? await updateCandidate(initial.id, body) : await createCandidate(body);
      let saved = res.data;
      if (!editing && resumeFile) {
        try {
          saved = (await uploadResume(saved.id, resumeFile)).data;
        } catch (uploadErr) {
          // The candidate exists now - don't lose that. Report the resume problem and continue.
          onSaved(saved, `Candidate created, but the resume was not uploaded: ${uploadErr.message}`);
          return;
        }
      }
      onSaved(saved);
    } catch (err) {
      setError(err.message);
      setErrors(err.errors || []);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{editing ? 'Edit candidate' : 'New candidate'}</h2>
        <ErrorBanner message={error} errors={errors} />
        <div className="form-grid">
          <label>
            First name
            <input value={form.firstName} onChange={set('firstName')} maxLength={50} required />
          </label>
          <label>
            Last name
            <input value={form.lastName} onChange={set('lastName')} maxLength={50} />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={set('email')} maxLength={150} required />
          </label>
          <label>
            Mobile (10 digits)
            <input value={form.mobile} onChange={set('mobile')} maxLength={10} inputMode="numeric" />
          </label>
          <label>
            Current company
            <input value={form.currentCompany} onChange={set('currentCompany')} maxLength={150} />
          </label>
          <label>
            Current designation
            <input value={form.currentDesignation} onChange={set('currentDesignation')} maxLength={100} />
          </label>
          <label>
            Current location
            <input value={form.currentLocation} onChange={set('currentLocation')} maxLength={100} />
          </label>
          <label>
            Total experience (years)
            <input type="number" min={0} max={60} step="0.1" value={form.totalExperience} onChange={set('totalExperience')} />
          </label>
          <label>
            Current CTC (annual)
            <input type="number" min={0} step="0.01" value={form.currentCtc} onChange={set('currentCtc')} />
          </label>
          <label>
            Expected CTC (annual)
            <input type="number" min={0} step="0.01" value={form.expectedCtc} onChange={set('expectedCtc')} />
          </label>
          <label>
            Notice period (days)
            <input type="number" min={0} max={365} value={form.noticePeriodDays} onChange={set('noticePeriodDays')} />
          </label>
          <label>
            Source
            <select value={form.source} onChange={set('source')}>
              {CANDIDATE_SOURCES.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
            </select>
          </label>
          {form.source === 'AGENCY' && (
            <label>
              Agency / vendor name
              <input value={form.vendorName} onChange={set('vendorName')} maxLength={150} />
            </label>
          )}
          {form.source === 'REFERRAL' && (
            <EmployeePicker label="Referred by" value={referrer} onChange={setReferrer} />
          )}
        </div>
        <label>
          Skills
          <textarea value={form.skills} onChange={set('skills')} maxLength={500} rows={2} placeholder="e.g. Java, Spring Boot, React, MySQL" />
        </label>

        {!editing && (
          <>
            <label>
              Resume (PDF, DOC or DOCX, max 5 MB) — optional, can be added later
              <input type="file" accept=".pdf,.doc,.docx" onChange={(e) => setResumeFile(e.target.files?.[0] || null)} />
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
              The candidate has consented to their personal data being stored and processed for hiring.
            </label>
          </>
        )}

        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </div>
  );
}
