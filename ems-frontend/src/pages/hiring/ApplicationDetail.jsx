import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getApplication, getApplicationHistory, getRequisition, getInterviewsByApplication, getOffersByApplication,
  moveApplication, cancelInterview, markInterviewStatus, getHiringLookups,
} from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge, ReasonModal } from '../../components/hiring/HiringBits';
import InterviewForm from '../../components/hiring/InterviewForm';
import OfferForm from '../../components/hiring/OfferForm';
import useHiringMe from '../../components/hiring/useHiringMe';
import { fmtDate, fmtDateTime, money, pretty } from '../../components/hiring/hiringUtils';

function InterviewCard({ interview, staff, onEdit, onCancel, onMark }) {
  return (
    <div className="feedback-box">
      <div className="inline-actions">
        <strong>Round {interview.roundNumber} · {interview.roundName}</strong>
        <StatusBadge status={interview.status} />
      </div>
      <div className="muted-text">
        {fmtDateTime(interview.scheduledAt)} · {interview.durationMinutes} min · {pretty(interview.mode)}
        {interview.locationOrLink ? ` · ${interview.locationOrLink}` : ''}
      </div>
      {interview.cancelReason && <div>Cancelled: {interview.cancelReason}</div>}
      <ul className="plain-list">
        {interview.panel.map((p) => (
          <li key={p.id}>
            <strong>{p.interviewerName}</strong>{' '}
            {p.submitted ? <span className="badge badge-success">Feedback in</span> : <span className="badge badge-muted">Pending</span>}
            {p.rating != null && (
              <div>
                Rating {p.rating}/5 · <StatusBadge status={p.recommendation} />
                {p.comments && <div className="muted-text">“{p.comments}”</div>}
              </div>
            )}
            {p.submitted && p.rating == null && (
              <div className="muted-text">Feedback is hidden until you submit your own.</div>
            )}
          </li>
        ))}
      </ul>
      {staff && interview.status === 'SCHEDULED' && (
        <div className="inline-actions">
          <button className="btn btn-secondary" onClick={() => onEdit(interview)}>Edit / reschedule</button>
          <button className="btn btn-secondary" onClick={() => onMark(interview, 'COMPLETED')}>Mark completed</button>
          <button className="btn btn-secondary" onClick={() => onMark(interview, 'NO_SHOW')}>No-show</button>
          <button className="btn btn-danger" onClick={() => onCancel(interview)}>Cancel</button>
        </div>
      )}
    </div>
  );
}

export default function ApplicationDetail() {
  const { id } = useParams();
  const me = useHiringMe();
  const [app, setApp] = useState(null);
  const [req, setReq] = useState(null);
  const [history, setHistory] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [offers, setOffers] = useState([]);
  const [lookups, setLookups] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [moveTo, setMoveTo] = useState('');
  const [moveRemarks, setMoveRemarks] = useState('');
  const [modal, setModal] = useState(null); // { type: 'schedule' | 'editInterview' | 'cancelInterview' | 'offer', interview? }

  const loadAll = useCallback(() => {
    getApplication(id)
      .then((res) => {
        setApp(res.data);
        // These can be forbidden for some viewers (e.g. a plain interviewer) - each fails independently.
        getRequisition(res.data.requisitionId).then((r) => setReq(r.data)).catch(() => setReq(null));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
    getApplicationHistory(id).then((res) => setHistory(res.data || [])).catch(() => {});
    getInterviewsByApplication(id).then((res) => setInterviews(res.data || [])).catch(() => setInterviews([]));
    getOffersByApplication(id).then((res) => setOffers(res.data || [])).catch(() => setOffers([]));
  }, [id]);

  useEffect(() => { loadAll(); }, [loadAll]);

  useEffect(() => {
    if (me?.staff) getHiringLookups().then((res) => setLookups(res.data)).catch(() => {});
  }, [me]);

  async function handleMove(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await moveApplication(id, moveTo, moveRemarks.trim() || null);
      setSuccess(`Moved to ${pretty(moveTo)}`);
      setMoveTo('');
      setMoveRemarks('');
      loadAll();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleMark(interview, status) {
    setError('');
    try {
      await markInterviewStatus(interview.id, status);
      setSuccess('Interview updated');
      loadAll();
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <Loading />;
  if (!app) return <div><ErrorBanner message={error || 'Application not found'} /></div>;

  const staff = !!me?.staff;
  const isHM = !!(me && req && req.hiringManagerId === me.employeeId);
  const canMove = staff || isHM;
  const closing = moveTo === 'REJECTED' || moveTo === 'WITHDRAWN';
  const hasOpenOffer = offers.some((o) => ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED'].includes(o.status));
  const canSchedule = staff && ['SCREENING', 'INTERVIEW'].includes(app.stage);
  const canMakeOffer = staff && app.stage === 'INTERVIEW' && !hasOpenOffer;

  return (
    <div>
      <p>{req ? <Link to={`/hiring/requisitions/${app.requisitionId}`}>← {app.requisitionCode}</Link> : <Link to="/hiring/interviews">← My interviews</Link>}</p>
      <div className="header-actions">
        <h1>{app.candidateName} <span className="muted-text">for {app.requisitionTitle}</span></h1>
        <StatusBadge status={app.stage} />
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="two-col stacked-panel">
        <div className="card-panel">
          <h2>Candidate</h2>
          <dl className="kv-grid">
            <dt>Email</dt><dd>{app.candidateEmail}</dd>
            <dt>Mobile</dt><dd>{app.candidateMobile || '—'}</dd>
            <dt>Experience</dt><dd>{app.candidateExperience ?? '—'} years</dd>
            <dt>Current company</dt><dd>{app.candidateCurrentCompany || '—'}</dd>
            <dt>Notice period</dt><dd>{app.candidateNoticePeriodDays ?? '—'} days</dd>
            {staff && (<><dt>Expected CTC</dt><dd>{money(app.candidateExpectedCtc)}</dd></>)}
            <dt>Applied on</dt><dd>{fmtDate(app.appliedOn)}</dd>
            {app.rejectionReason && (<><dt>Closed because</dt><dd>{app.rejectionReason}</dd></>)}
            {app.hiredEmployeeId && (<><dt>Employee record</dt><dd>#{app.hiredEmployeeId}</dd></>)}
          </dl>
          <p><Link to={`/hiring/candidates/${app.candidateId}`}>Open full candidate profile &amp; resume</Link></p>

          {canMove && app.allowedNextStages?.length > 0 && (
            <form className="card-form" onSubmit={handleMove}>
              <h2>Move in pipeline</h2>
              <label>
                Move to
                <select value={moveTo} onChange={(e) => setMoveTo(e.target.value)} required>
                  <option value="" disabled>Select a stage…</option>
                  {app.allowedNextStages.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
                </select>
              </label>
              <label>
                {closing ? 'Reason (required)' : 'Remarks (optional)'}
                <input value={moveRemarks} onChange={(e) => setMoveRemarks(e.target.value)} maxLength={500} required={closing} />
              </label>
              <button className="btn btn-primary" type="submit" disabled={!moveTo}>Move</button>
              <p className="hint-text">Interview → schedule rounds below. Offer → create an offer below. Hired → convert the accepted offer.</p>
            </form>
          )}
        </div>

        <div className="card-panel">
          <h2>History</h2>
          <ol className="approval-timeline">
            {history.map((h) => (
              <li key={h.id} className="approval-step approval-approved">
                {h.fromStage ? <>{pretty(h.fromStage)} → <strong>{pretty(h.toStage)}</strong></> : <strong>{pretty(h.toStage)}</strong>}
                <div className="muted-text">{fmtDateTime(h.changedAt)}{h.changedByName ? ` · ${h.changedByName}` : ' · system'}</div>
                {h.remarks && <div>{h.remarks}</div>}
              </li>
            ))}
          </ol>
        </div>
      </div>

      <div className="card-panel stacked-panel">
        <div className="header-actions">
          <h2>Interviews</h2>
          {canSchedule && <button className="btn btn-primary" onClick={() => setModal({ type: 'schedule' })}>Schedule interview</button>}
        </div>
        {interviews.length === 0 && <p className="muted-text">No interviews yet.</p>}
        {interviews.map((i) => (
          <InterviewCard
            key={i.id}
            interview={i}
            staff={staff}
            onEdit={(iv) => setModal({ type: 'editInterview', interview: iv })}
            onCancel={(iv) => setModal({ type: 'cancelInterview', interview: iv })}
            onMark={handleMark}
          />
        ))}
      </div>

      {(staff || isHM) && (
        <div className="card-panel stacked-panel">
          <div className="header-actions">
            <h2>Offers</h2>
            {canMakeOffer && <button className="btn btn-primary" onClick={() => setModal({ type: 'offer' })}>Draft offer</button>}
          </div>
          {app.stage === 'INTERVIEW' && !hasOpenOffer && !interviews.some((i) => i.status === 'COMPLETED') && (
            <p className="hint-text">An offer can be drafted once at least one interview round is completed (all panelists have submitted feedback, or you mark it completed).</p>
          )}
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Created</th><th>Designation</th><th>Annual CTC</th><th>Joining</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {offers.length === 0 && <tr><td colSpan={6} className="empty-row">No offers yet.</td></tr>}
                {offers.map((o) => (
                  <tr key={o.id}>
                    <td>{fmtDate(o.createdAt)}</td>
                    <td>{o.designationName}</td>
                    <td>{money(o.annualCtc)}</td>
                    <td>{fmtDate(o.joiningDate)}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td><Link to={`/hiring/offers/${o.id}`}>Open</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {modal?.type === 'schedule' && (
        <InterviewForm
          applicationId={Number(id)}
          onCancel={() => setModal(null)}
          onSaved={() => { setModal(null); setSuccess('Interview scheduled'); loadAll(); }}
        />
      )}
      {modal?.type === 'editInterview' && (
        <InterviewForm
          initial={modal.interview}
          onCancel={() => setModal(null)}
          onSaved={() => { setModal(null); setSuccess('Interview updated'); loadAll(); }}
        />
      )}
      {modal?.type === 'cancelInterview' && (
        <ReasonModal title="Cancel interview" label="Reason (optional)" confirmLabel="Cancel interview"
          onCancel={() => setModal(null)}
          onSubmit={(text) => cancelInterview(modal.interview.id, text).then(() => { setModal(null); setSuccess('Interview cancelled'); loadAll(); })} />
      )}
      {modal?.type === 'offer' && (
        <OfferForm
          applicationId={Number(id)}
          requisition={req}
          lookups={lookups}
          onCancel={() => setModal(null)}
          onSaved={() => { setModal(null); setSuccess('Offer drafted'); loadAll(); }}
        />
      )}
    </div>
  );
}
