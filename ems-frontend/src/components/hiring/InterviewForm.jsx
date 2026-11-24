import { useState } from 'react';
import { scheduleInterview, updateInterview } from '../../api/hiring';
import { EmployeeMultiPicker } from './HiringBits';
import { INTERVIEW_MODES, emptyToNull, pretty, toLocalInput } from './hiringUtils';
import { ErrorBanner } from '../Feedback';

// Schedule (applicationId) or edit (initial = InterviewDTO) an interview round.
export default function InterviewForm({ applicationId, initial, onSaved, onCancel }) {
  const editing = !!initial;
  const [form, setForm] = useState({
    roundName: initial?.roundName || '',
    mode: initial?.mode || 'VIDEO',
    scheduledAt: toLocalInput(initial?.scheduledAt),
    durationMinutes: initial?.durationMinutes ?? 60,
    locationOrLink: initial?.locationOrLink || '',
  });
  const [panel, setPanel] = useState(
    (initial?.panel || []).map((p) => ({ id: p.interviewerId, label: p.interviewerName })),
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState([]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  async function handleSubmit(e) {
    e.preventDefault();
    if (panel.length === 0) {
      setError('Add at least one interviewer to the panel');
      return;
    }
    setBusy(true);
    setError('');
    setErrors([]);
    const body = {
      roundName: form.roundName.trim(),
      mode: form.mode,
      scheduledAt: form.scheduledAt,
      durationMinutes: Number(form.durationMinutes),
      locationOrLink: emptyToNull(form.locationOrLink.trim()),
      interviewerIds: panel.map((p) => p.id),
    };
    try {
      const res = editing ? await updateInterview(initial.id, body) : await scheduleInterview(applicationId, body);
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
        <h2>{editing ? `Edit round ${initial.roundNumber}` : 'Schedule interview'}</h2>
        <ErrorBanner message={error} errors={errors} />
        <div className="form-grid">
          <label>
            Round name
            <input value={form.roundName} onChange={set('roundName')} maxLength={100} placeholder="Technical, Managerial, HR…" required />
          </label>
          <label>
            Mode
            <select value={form.mode} onChange={set('mode')}>
              {INTERVIEW_MODES.map((m) => <option key={m} value={m}>{pretty(m)}</option>)}
            </select>
          </label>
          <label>
            Date and time
            <input type="datetime-local" value={form.scheduledAt} onChange={set('scheduledAt')} required />
          </label>
          <label>
            Duration (minutes)
            <input type="number" min={15} max={480} step={5} value={form.durationMinutes} onChange={set('durationMinutes')} required />
          </label>
        </div>
        <label>
          Meeting link or location
          <input value={form.locationOrLink} onChange={set('locationOrLink')} maxLength={300} />
        </label>
        <EmployeeMultiPicker label="Interview panel" selected={panel} onChange={setPanel} />
        <p className="hint-text">The candidate and each panelist are e-mailed the details.</p>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </div>
  );
}
