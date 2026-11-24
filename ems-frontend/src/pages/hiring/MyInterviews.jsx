import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyInterviews, submitFeedback } from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge } from '../../components/hiring/HiringBits';
import useHiringMe from '../../components/hiring/useHiringMe';
import { RECOMMENDATIONS, fmtDateTime, pretty } from '../../components/hiring/hiringUtils';

function FeedbackModal({ interview, existing, onSaved, onCancel }) {
  const [rating, setRating] = useState(existing?.rating ?? 3);
  const [recommendation, setRecommendation] = useState(existing?.recommendation || 'HIRE');
  const [comments, setComments] = useState(existing?.comments || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await submitFeedback(interview.id, { rating: Number(rating), recommendation, comments: comments.trim() || null });
      onSaved();
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>Feedback — {interview.candidateName}</h2>
        <p className="muted-text">Round {interview.roundNumber} · {interview.roundName}</p>
        <ErrorBanner message={error} />
        <label>
          Rating
          <select value={rating} onChange={(e) => setRating(e.target.value)}>
            <option value={1}>1 — Poor</option>
            <option value={2}>2 — Below expectations</option>
            <option value={3}>3 — Meets expectations</option>
            <option value={4}>4 — Strong</option>
            <option value={5}>5 — Excellent</option>
          </select>
        </label>
        <label>
          Recommendation
          <select value={recommendation} onChange={(e) => setRecommendation(e.target.value)}>
            {RECOMMENDATIONS.map((r) => <option key={r} value={r}>{pretty(r)}</option>)}
          </select>
        </label>
        <label>
          Comments
          <textarea value={comments} onChange={(e) => setComments(e.target.value)} maxLength={2000} rows={5} />
        </label>
        <p className="hint-text">Other panelists&apos; feedback becomes visible to you after you submit yours.</p>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Submit feedback'}</button>
        </div>
      </form>
    </div>
  );
}

export default function MyInterviews() {
  const me = useHiringMe();
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [feedbackFor, setFeedbackFor] = useState(null);

  const load = useCallback(() => {
    getMyInterviews()
      .then((res) => setItems(res.data || []))
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => { load(); }, [load]);

  if (!items) return <div><h1>My Interviews</h1><ErrorBanner message={error} />{!error && <Loading />}</div>;

  const mine = (i) => i.panel.find((p) => p.interviewerId === me?.employeeId);

  return (
    <div>
      <h1>My Interviews</h1>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />
      <div className="card-panel">
        <div className="data-table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Candidate</th><th>Position</th><th>Round</th><th>When</th><th>Mode</th><th>Status</th><th>My feedback</th><th></th></tr>
            </thead>
            <tbody>
              {items.length === 0 && <tr><td colSpan={8} className="empty-row">You are not on any interview panel.</td></tr>}
              {items.map((i) => {
                const m = mine(i);
                const canGive = i.status === 'SCHEDULED' || i.status === 'COMPLETED';
                return (
                  <tr key={i.id}>
                    <td><Link to={`/hiring/candidates/${i.candidateId}`}>{i.candidateName}</Link></td>
                    <td>{i.requisitionTitle}</td>
                    <td>{i.roundNumber} · {i.roundName}</td>
                    <td>{fmtDateTime(i.scheduledAt)}<div className="muted-text">{i.locationOrLink || ''}</div></td>
                    <td>{pretty(i.mode)}</td>
                    <td><StatusBadge status={i.status} /></td>
                    <td>{m?.submitted ? <span className="badge badge-success">Submitted</span> : <span className="badge badge-muted">Pending</span>}</td>
                    <td>
                      <div className="row-actions">
                        {canGive && <button className="btn btn-secondary" onClick={() => setFeedbackFor(i)}>{m?.submitted ? 'Edit feedback' : 'Give feedback'}</button>}
                        <Link to={`/hiring/applications/${i.applicationId}`}>Details</Link>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {feedbackFor && (
        <FeedbackModal
          interview={feedbackFor}
          existing={mine(feedbackFor)?.submitted ? mine(feedbackFor) : null}
          onCancel={() => setFeedbackFor(null)}
          onSaved={() => { setFeedbackFor(null); setSuccess('Feedback submitted'); load(); }}
        />
      )}
    </div>
  );
}
