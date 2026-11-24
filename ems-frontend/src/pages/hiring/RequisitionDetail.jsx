import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getRequisition, getHiringLookups, submitRequisition, approveRequisition, rejectRequisition,
  changeRequisitionStatus, assignRecruiter, getApplicationsByRequisition, searchCandidates, applyCandidate,
} from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge, ApprovalTimeline, ReasonModal } from '../../components/hiring/HiringBits';
import RequisitionForm from '../../components/hiring/RequisitionForm';
import useHiringMe from '../../components/hiring/useHiringMe';
import { PIPELINE_STAGES, fmtDate, money, pretty } from '../../components/hiring/hiringUtils';

// Pick a candidate (type-ahead) and add them to this requisition's pipeline.
function AddCandidateModal({ requisitionId, onAdded, onCancel }) {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    if (keyword.trim().length < 2) return undefined;
    let cancelled = false;
    const t = setTimeout(() => {
      searchCandidates({ keyword: keyword.trim(), page: 0, size: 8 })
        .then((res) => { if (!cancelled) setResults(res.data?.content || []); })
        .catch((err) => { if (!cancelled) setError(err.message); });
    }, 300);
    return () => { cancelled = true; clearTimeout(t); };
  }, [keyword]);

  async function add(candidate) {
    setBusyId(candidate.id);
    setError('');
    try {
      await applyCandidate(candidate.id, requisitionId);
      onAdded(candidate);
    } catch (err) {
      setError(err.message);
      setBusyId(null);
    }
  }

  const shown = keyword.trim().length < 2 ? [] : results;

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <h2>Add candidate to this requisition</h2>
        <ErrorBanner message={error} />
        <label>
          Search candidates (name, email, mobile, skills)
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} autoFocus />
        </label>
        <ul className="plain-list">
          {shown.map((c) => (
            <li key={c.id} className="inline-actions">
              <span>{c.firstName} {c.lastName} <span className="muted-text">{c.email}</span></span>
              <button className="btn btn-secondary" disabled={busyId === c.id} onClick={() => add(c)}>Add</button>
            </li>
          ))}
          {keyword.trim().length >= 2 && shown.length === 0 && <li className="muted-text">No matching candidates. Create the candidate first under Candidates.</li>}
        </ul>
        <div className="modal-actions">
          <button className="btn btn-link" onClick={onCancel}>Close</button>
        </div>
      </div>
    </div>
  );
}

export default function RequisitionDetail() {
  const { id } = useParams();
  const me = useHiringMe();
  const [req, setReq] = useState(null);
  const [lookups, setLookups] = useState(null);
  const [apps, setApps] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [modal, setModal] = useState(null); // 'edit' | 'reject' | 'hold' | 'close' | 'cancel' | 'add'
  const [recruiterId, setRecruiterId] = useState('');

  const load = useCallback(() => {
    getRequisition(id)
      .then((res) => setReq(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const canSeePipeline = !!me && !!req && (me.staff || req.hiringManagerId === me.employeeId);

  const loadApps = useCallback(() => {
    getApplicationsByRequisition(id)
      .then((res) => setApps(res.data || []))
      .catch((err) => setError(err.message));
  }, [id]);

  useEffect(() => { if (canSeePipeline) loadApps(); }, [canSeePipeline, loadApps]);

  async function run(action, okMessage) {
    setError('');
    setSuccess('');
    try {
      const res = await action();
      setReq(res.data);
      setSuccess(okMessage);
      setModal(null);
    } catch (err) {
      setError(err.message);
      setModal(null);
    }
  }

  async function openEdit() {
    try {
      setLookups((await getHiringLookups()).data);
      setModal('edit');
    } catch (err) {
      setError(err.message);
    }
  }

  // Lookups (incl. recruiters) are needed for assigning a recruiter by staff.
  useEffect(() => {
    if (me?.staff) getHiringLookups().then((res) => setLookups(res.data)).catch(() => {});
  }, [me]);

  if (loading) return <Loading />;
  if (!req) return <div><ErrorBanner message={error || 'Requisition not found'} /></div>;

  const st = req.status;
  const stageCounts = PIPELINE_STAGES.map((s) => [s, (apps || []).filter((a) => a.stage === s).length]);

  return (
    <div>
      <p><Link to="/hiring/requisitions">← Requisitions</Link></p>
      <div className="header-actions">
        <h1>{req.requisitionCode} · {req.title}</h1>
        <StatusBadge status={st} />
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="card-panel">
        <div className="inline-actions">
          {req.viewerCanEdit && <button className="btn btn-secondary" onClick={openEdit}>Edit</button>}
          {req.viewerCanEdit && st === 'DRAFT' && (
            <button className="btn btn-primary" onClick={() => run(() => submitRequisition(id), 'Submitted for approval')}>Submit for approval</button>
          )}
          {req.viewerCanApprove && (
            <>
              <button className="btn btn-primary" onClick={() => run(() => approveRequisition(id, ''), 'Approved')}>Approve</button>
              <button className="btn btn-danger" onClick={() => setModal('reject')}>Reject</button>
            </>
          )}
          {me?.staff && st === 'APPROVED' && (
            <button className="btn btn-primary" onClick={() => run(() => changeRequisitionStatus(id, 'OPEN'), 'Requisition is now open for hiring')}>Open for hiring</button>
          )}
          {req.viewerCanManage && st === 'OPEN' && <button className="btn btn-secondary" onClick={() => setModal('hold')}>Put on hold</button>}
          {req.viewerCanManage && st === 'ON_HOLD' && (
            <button className="btn btn-secondary" onClick={() => run(() => changeRequisitionStatus(id, 'RESUME'), 'Resumed')}>Resume</button>
          )}
          {req.viewerCanManage && (st === 'OPEN' || st === 'ON_HOLD') && <button className="btn btn-secondary" onClick={() => setModal('close')}>Close</button>}
          {req.viewerCanManage && ['DRAFT', 'PENDING_APPROVAL', 'APPROVED'].includes(st) && (
            <button className="btn btn-danger" onClick={() => setModal('cancel')}>Cancel requisition</button>
          )}
        </div>
        {st === 'REJECTED' && req.viewerCanEdit && (
          <p className="hint-text">Rejected. Edit the requisition to address the feedback — it returns to draft and can be submitted again.</p>
        )}
      </div>

      <div className="two-col stacked-panel">
        <div className="card-panel">
          <h2>Details</h2>
          <dl className="kv-grid">
            <dt>Department</dt><dd>{req.departmentName}</dd>
            <dt>Designation</dt><dd>{req.designationName}</dd>
            <dt>Employee type</dt><dd>{req.employeeTypeName}</dd>
            <dt>Location</dt><dd>{req.locationName || '—'}</dd>
            <dt>Openings</dt><dd>{req.filledCount} filled of {req.openings}</dd>
            <dt>Budget (annual CTC)</dt><dd>{money(req.budgetMin)} – {money(req.budgetMax)}</dd>
            <dt>Experience (years)</dt><dd>{req.minExperience ?? '—'} – {req.maxExperience ?? '—'}</dd>
            <dt>Target close date</dt><dd>{fmtDate(req.targetCloseDate)}</dd>
            <dt>Hiring manager</dt><dd>{req.hiringManagerName}</dd>
            <dt>Requested by</dt><dd>{req.requestedByName}</dd>
            <dt>Recruiter</dt>
            <dd>
              {req.recruiterName || '— not assigned —'}
              {me?.staff && lookups?.recruiters && req.status !== 'CLOSED' && req.status !== 'CANCELLED' && (
                <span className="inline-actions">
                  <select value={recruiterId} onChange={(e) => setRecruiterId(e.target.value)}>
                    <option value="">Assign recruiter…</option>
                    {lookups.recruiters.map((r) => <option key={r.id} value={r.id}>{r.fullName}</option>)}
                  </select>
                  <button className="btn btn-secondary" disabled={!recruiterId} onClick={() => run(() => assignRecruiter(id, Number(recruiterId)), 'Recruiter assigned')}>Assign</button>
                </span>
              )}
            </dd>
            {req.statusRemarks && (<><dt>Last remarks</dt><dd>{req.statusRemarks}</dd></>)}
          </dl>
          {req.jobDescription && (<><h2>Job description</h2><p style={{ whiteSpace: 'pre-wrap' }}>{req.jobDescription}</p></>)}
          {req.justification && (<><h2>Justification</h2><p style={{ whiteSpace: 'pre-wrap' }}>{req.justification}</p></>)}
        </div>
        <div className="card-panel">
          <h2>Approvals</h2>
          <ApprovalTimeline approvals={req.approvals} />
        </div>
      </div>

      {canSeePipeline && (
        <div className="card-panel stacked-panel">
          <div className="header-actions">
            <h2>Candidate pipeline</h2>
            {me?.staff && st === 'OPEN' && <button className="btn btn-primary" onClick={() => setModal('add')}>Add candidate</button>}
          </div>
          {apps && (
            <p className="muted-text">
              {stageCounts.filter(([, n]) => n > 0).map(([s, n]) => `${pretty(s)}: ${n}`).join(' · ') || 'No candidates yet.'}
            </p>
          )}
          {!apps ? <Loading /> : (
            <div className="data-table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Candidate</th><th>Experience</th><th>Notice</th>{me?.staff && <th>Expected CTC</th>}<th>Applied</th><th>Stage</th></tr>
                </thead>
                <tbody>
                  {apps.length === 0 && <tr><td colSpan={6} className="empty-row">No candidates in this requisition yet.</td></tr>}
                  {apps.map((a) => (
                    <tr key={a.id}>
                      <td><Link to={`/hiring/applications/${a.id}`}>{a.candidateName}</Link></td>
                      <td>{a.candidateExperience ?? '—'} yrs</td>
                      <td>{a.candidateNoticePeriodDays ?? '—'} d</td>
                      {me?.staff && <td>{money(a.candidateExpectedCtc)}</td>}
                      <td>{fmtDate(a.appliedOn)}</td>
                      <td><StatusBadge status={a.stage} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {modal === 'edit' && (
        <RequisitionForm
          lookups={lookups}
          initial={req}
          onCancel={() => setModal(null)}
          onSaved={(saved) => { setReq(saved); setModal(null); setSuccess('Requisition updated'); }}
        />
      )}
      {modal === 'reject' && (
        <ReasonModal title="Reject requisition" label="Reason (required)" required confirmLabel="Reject"
          onCancel={() => setModal(null)}
          onSubmit={(text) => rejectRequisition(id, text).then((res) => { setReq(res.data); setSuccess('Requisition rejected'); setModal(null); })} />
      )}
      {modal === 'hold' && (
        <ReasonModal title="Put on hold" label="Reason (optional)" confirmLabel="Put on hold"
          onCancel={() => setModal(null)}
          onSubmit={(text) => changeRequisitionStatus(id, 'HOLD', text).then((res) => { setReq(res.data); setSuccess('Requisition on hold'); setModal(null); })} />
      )}
      {modal === 'close' && (
        <ReasonModal title="Close requisition" label="Reason (optional)" confirmLabel="Close requisition"
          onCancel={() => setModal(null)}
          onSubmit={(text) => changeRequisitionStatus(id, 'CLOSE', text).then((res) => { setReq(res.data); setSuccess('Requisition closed'); setModal(null); })} />
      )}
      {modal === 'cancel' && (
        <ReasonModal title="Cancel requisition" label="Reason (optional)" confirmLabel="Cancel requisition"
          onCancel={() => setModal(null)}
          onSubmit={(text) => changeRequisitionStatus(id, 'CANCEL', text).then((res) => { setReq(res.data); setSuccess('Requisition cancelled'); setModal(null); })} />
      )}
      {modal === 'add' && (
        <AddCandidateModal
          requisitionId={Number(id)}
          onCancel={() => setModal(null)}
          onAdded={(c) => { setModal(null); setSuccess(`${c.firstName} added to the pipeline`); loadApps(); load(); }}
        />
      )}
    </div>
  );
}
