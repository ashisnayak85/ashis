import { useEffect, useState } from 'react';
import { searchHiringEmployees } from '../../api/hiring';
import { badgeClass, fmtDateTime, pretty } from './hiringUtils';

export function StatusBadge({ status }) {
  if (!status) return null;
  return <span className={`badge ${badgeClass(status)}`}>{pretty(status)}</span>;
}

// Frozen approval stages of a requisition / offer.
export function ApprovalTimeline({ approvals }) {
  if (!approvals || approvals.length === 0) {
    return <p className="muted-text">No approval stages (none configured, or not submitted yet).</p>;
  }
  return (
    <ol className="approval-timeline">
      {approvals.map((a) => (
        <li key={a.id} className={`approval-step approval-${a.status.toLowerCase()}`}>
          <strong>{a.stageName}</strong> — <StatusBadge status={a.status} />
          <div className="muted-text">
            {a.approverName ? `Approver: ${a.approverName}` : 'No approver'}
            {a.actedAt ? ` · ${fmtDateTime(a.actedAt)}` : ''}
          </div>
          {a.remarks && <div>“{a.remarks}”</div>}
        </li>
      ))}
    </ol>
  );
}

// Asks for a reason / remarks before a destructive or decision action (reject, revoke, decline...).
export function ReasonModal({ title, label, required, confirmLabel, onSubmit, onCancel }) {
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await onSubmit(text.trim());
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card modal-card-small" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>{title}</h2>
        {error && <div className="banner banner-error">{error}</div>}
        <label>
          {label}
          <textarea value={text} onChange={(e) => setText(e.target.value)} maxLength={500} required={required} autoFocus />
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Please wait…' : confirmLabel || 'Confirm'}</button>
        </div>
      </form>
    </div>
  );
}

// Type-ahead employee search. value = { id, label } | null
export function EmployeePicker({ label, value, onChange, placeholder }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [open, setOpen] = useState(false);

  const searchable = query.trim().length >= 2;

  useEffect(() => {
    if (!searchable) return undefined;
    let cancelled = false;
    const timer = setTimeout(() => {
      searchHiringEmployees(query.trim())
        .then((res) => { if (!cancelled) setResults(res.data || []); })
        .catch(() => { if (!cancelled) setResults([]); });
    }, 300);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [query, searchable]);

  // Derived, not stored: results only count while the query is long enough.
  const shown = searchable ? results : [];

  function pick(emp) {
    onChange({ id: emp.id, label: `${emp.fullName} (${emp.employeeCode})` });
    setQuery('');
    setOpen(false);
  }

  return (
    <div className="picker">
      <label>
        {label}
        {value ? (
          <div className="picker-selected">
            <span>{value.label}</span>
            <button type="button" className="btn btn-link" onClick={() => onChange(null)}>Change</button>
          </div>
        ) : (
          <input
            type="text"
            value={query}
            placeholder={placeholder || 'Type at least 2 letters of a name…'}
            onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
            onFocus={() => setOpen(true)}
          />
        )}
      </label>
      {!value && open && shown.length > 0 && (
        <ul className="picker-list">
          {shown.map((emp) => (
            <li key={emp.id}>
              <button type="button" onClick={() => pick(emp)}>
                {emp.fullName} <span className="muted-text">{emp.employeeCode} · {emp.departmentName || '—'}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

// Several employees (interview panel). selected = [{ id, label }]
export function EmployeeMultiPicker({ label, selected, onChange }) {
  const [current, setCurrent] = useState(null);

  function handlePick(v) {
    if (v && !selected.some((s) => s.id === v.id)) {
      onChange([...selected, v]);
    }
    setCurrent(null);
  }

  return (
    <div>
      <EmployeePicker label={label} value={current} onChange={handlePick} placeholder="Search and add an interviewer…" />
      {selected.length > 0 && (
        <div className="chip-row">
          {selected.map((s) => (
            <span key={s.id} className="file-chip">
              {s.label}
              <button type="button" className="chip-remove" onClick={() => onChange(selected.filter((x) => x.id !== s.id))} aria-label={`Remove ${s.label}`}>×</button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
