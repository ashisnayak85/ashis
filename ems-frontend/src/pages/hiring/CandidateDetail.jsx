import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getCandidate, getApplicationsByCandidate, searchRequisitions, applyCandidate,
  uploadResume, fetchResumeBlob, anonymiseCandidate,
} from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge } from '../../components/hiring/HiringBits';
import CandidateForm from '../../components/hiring/CandidateForm';
import useHiringMe from '../../components/hiring/useHiringMe';
import { downloadBlob, fmtDate, fmtDateTime, money, openBlobInNewTab, pretty } from '../../components/hiring/hiringUtils';

export default function CandidateDetail() {
  const { id } = useParams();
  const me = useHiringMe();
  const [candidate, setCandidate] = useState(null);
  const [apps, setApps] = useState([]);
  const [openReqs, setOpenReqs] = useState([]);
  const [reqId, setReqId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editing, setEditing] = useState(false);
  const fileInput = useRef(null);

  const load = useCallback(() => {
    getCandidate(id)
      .then((res) => setCandidate(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const loadStaffData = useCallback(() => {
    getApplicationsByCandidate(id).then((res) => setApps(res.data || [])).catch(() => {});
    searchRequisitions({ status: 'OPEN', page: 0, size: 100 }).then((res) => setOpenReqs(res.data?.content || [])).catch(() => {});
  }, [id]);

  useEffect(() => { if (me?.staff) loadStaffData(); }, [me, loadStaffData]);

  async function handleResumeView() {
    setError('');
    try {
      const isPdf = (candidate.resumeFileName || '').toLowerCase().endsWith('.pdf');
      if (isPdf) {
        await openBlobInNewTab(() => fetchResumeBlob(id));
      } else {
        await downloadBlob(() => fetchResumeBlob(id), candidate.resumeFileName);
      }
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleUpload(e) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError('');
    try {
      setCandidate((await uploadResume(id, file)).data);
      setSuccess('Resume uploaded');
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleApply() {
    setError('');
    try {
      await applyCandidate(Number(id), Number(reqId));
      setSuccess('Candidate added to the requisition');
      setReqId('');
      loadStaffData();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAnonymise() {
    if (!window.confirm('Permanently remove this candidate\'s personal data and resume? This cannot be undone.')) return;
    setError('');
    try {
      setCandidate((await anonymiseCandidate(id)).data);
      setSuccess('Candidate data anonymised');
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <Loading />;
  if (!candidate) return <div><ErrorBanner message={error || 'Candidate not found'} /></div>;

  const staff = !!me?.staff;
  const active = candidate.active !== false;
  const alreadyApplied = new Set(apps.map((a) => a.requisitionId));
  const applicable = openReqs.filter((r) => !alreadyApplied.has(r.id));

  return (
    <div>
      <p>{staff ? <Link to="/hiring/candidates">← Candidates</Link> : <Link to="/hiring/interviews">← My interviews</Link>}</p>
      <div className="header-actions">
        <h1>{candidate.firstName} {candidate.lastName}</h1>
        {staff && active && <button className="btn btn-secondary" onClick={() => setEditing(true)}>Edit</button>}
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />
      {!active && <div className="banner banner-error">This candidate&apos;s data has been anonymised.</div>}
      {candidate.existingEmployeeId && (
        <div className="banner banner-error">
          An employee with this email already exists (employee #{candidate.existingEmployeeId}). If this is a rehire,
          converting an offer will be blocked until that is resolved.
        </div>
      )}

      <div className="two-col stacked-panel">
        <div className="card-panel">
          <h2>Profile</h2>
          <dl className="kv-grid">
            <dt>Email</dt><dd>{candidate.email}</dd>
            <dt>Mobile</dt><dd>{candidate.mobile || '—'}</dd>
            <dt>Current company</dt><dd>{candidate.currentCompany || '—'}</dd>
            <dt>Current designation</dt><dd>{candidate.currentDesignation || '—'}</dd>
            <dt>Current location</dt><dd>{candidate.currentLocation || '—'}</dd>
            <dt>Experience</dt><dd>{candidate.totalExperience ?? '—'} years</dd>
            <dt>Notice period</dt><dd>{candidate.noticePeriodDays ?? '—'} days</dd>
            {staff && (<><dt>Current CTC</dt><dd>{money(candidate.currentCtc)}</dd><dt>Expected CTC</dt><dd>{money(candidate.expectedCtc)}</dd></>)}
            <dt>Skills</dt><dd>{candidate.skills || '—'}</dd>
            <dt>Source</dt>
            <dd>{pretty(candidate.source)}{candidate.vendorName ? ` · ${candidate.vendorName}` : ''}{candidate.referredByName ? ` · by ${candidate.referredByName}` : ''}</dd>
            <dt>Consent recorded</dt><dd>{fmtDateTime(candidate.consentGivenAt)}</dd>
          </dl>
        </div>

        <div className="card-panel">
          <h2>Resume</h2>
          {candidate.resumeAvailable ? (
            <p>
              <strong>{candidate.resumeFileName}</strong><br />
              <span className="muted-text">Uploaded {fmtDateTime(candidate.resumeUploadedAt)}</span>
            </p>
          ) : <p className="muted-text">No resume uploaded.</p>}
          <div className="inline-actions">
            {candidate.resumeAvailable && <button className="btn btn-primary" onClick={handleResumeView}>View / download</button>}
            {staff && active && (
              <>
                <button className="btn btn-secondary" onClick={() => fileInput.current?.click()}>
                  {candidate.resumeAvailable ? 'Replace resume' : 'Upload resume'}
                </button>
                <input ref={fileInput} type="file" accept=".pdf,.doc,.docx" hidden onChange={handleUpload} />
              </>
            )}
          </div>
          <p className="hint-text">PDF, DOC or DOCX, up to 5 MB. Resumes are private: only people involved in hiring this candidate can open them.</p>
        </div>
      </div>

      {staff && (
        <div className="card-panel stacked-panel">
          <h2>Applications</h2>
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Requisition</th><th>Applied</th><th>Stage</th><th></th></tr></thead>
              <tbody>
                {apps.length === 0 && <tr><td colSpan={4} className="empty-row">Not added to any requisition yet.</td></tr>}
                {apps.map((a) => (
                  <tr key={a.id}>
                    <td><Link to={`/hiring/requisitions/${a.requisitionId}`}>{a.requisitionCode}</Link> · {a.requisitionTitle}</td>
                    <td>{fmtDate(a.appliedOn)}</td>
                    <td><StatusBadge status={a.stage} /></td>
                    <td><Link to={`/hiring/applications/${a.id}`}>Open</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {active && (
            <div className="inline-actions">
              <select value={reqId} onChange={(e) => setReqId(e.target.value)}>
                <option value="">Add to an open requisition…</option>
                {applicable.map((r) => <option key={r.id} value={r.id}>{r.requisitionCode} · {r.title}</option>)}
              </select>
              <button className="btn btn-primary" disabled={!reqId} onClick={handleApply}>Add</button>
            </div>
          )}
        </div>
      )}

      {me?.admin && active && (
        <div className="card-panel stacked-panel">
          <h2>Data retention</h2>
          <p className="muted-text">Remove this person&apos;s personal data and resume (for deletion requests or retention limits). Their application history stays, without personal details. Only possible when they have no open applications.</p>
          <div><button className="btn btn-danger" onClick={handleAnonymise}>Anonymise candidate</button></div>
        </div>
      )}

      {editing && (
        <CandidateForm
          initial={candidate}
          onCancel={() => setEditing(false)}
          onSaved={(saved) => { setCandidate(saved); setEditing(false); setSuccess('Candidate updated'); }}
        />
      )}
    </div>
  );
}
