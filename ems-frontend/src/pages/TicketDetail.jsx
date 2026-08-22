import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  getTicket, getConversation, markRead, reply,
  claimTicket, resolveTicket, rejectTicket, transferTicket,
  acceptResolution, escalateTicket, closeTicket,
} from '../api/tickets';
import { getTeam } from '../api/ticketTeams';
import { Loading, ErrorBanner, SuccessBanner } from '../components/Feedback';
import { TicketStatusBadge, TicketPriorityBadge } from '../components/TicketBadges';
import { useAuth } from '../context/AuthContext';

const ENTRY_LABELS = {
  CLAIM: (e) => `${e.authorName} claimed this ticket`,
  RESOLVE: (e) => `${e.authorName} marked this ticket resolved`,
  REJECT: (e) => `${e.authorName} rejected this ticket`,
  TRANSFER: (e) => `${e.authorName} transferred this ticket to ${e.targetEmployeeName}`,
  ESCALATE: (e) => `${e.authorName} escalated this ticket to ${e.targetEmployeeName}`,
  ACCEPT: (e) => `${e.authorName} accepted the outcome - ticket closed`,
  CLOSE: (e) => `${e.authorName} closed this ticket`,
  SLA_BREACH: () => 'SLA breach recorded',
};

function ConversationEntry({ entry, myEmployeeId }) {
  if (entry.entryType === 'REPLY') {
    const mine = entry.authorId === myEmployeeId;
    return (
      <div className={`conversation-entry-reply${mine ? ' mine' : ''}`}>
        <div className="conversation-entry-author">{entry.authorName}</div>
        <div>{entry.message}</div>
        <div className="conversation-entry-time">{new Date(entry.createdAt).toLocaleString()}</div>
      </div>
    );
  }
  const label = (ENTRY_LABELS[entry.entryType] || (() => entry.entryType))(entry);
  return (
    <div className={`conversation-entry-system entry-${entry.entryType.toLowerCase()}`}>
      {label}
      {entry.message && <> — {entry.message}</>}
    </div>
  );
}

// Generic confirm-with-message modal, reused by resolve/reject/transfer/escalate/close.
function ActionModal({ title, requireMessage, messageLabel = 'Message', children, onCancel, onConfirm }) {
  const [message, setMessage] = useState('');
  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => { e.preventDefault(); onConfirm(message); }}
      >
        <h2>{title}</h2>
        <div className="form-grid">
          {children}
          <label>
            {messageLabel}{requireMessage ? ' (required)' : ' (optional)'}
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={4} maxLength={2000} required={requireMessage} />
          </label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary">Confirm</button>
        </div>
      </form>
    </div>
  );
}

export default function TicketDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const myEmployeeId = user?.employeeId;

  const [ticket, setTicket] = useState(null);
  const [conversation, setConversation] = useState([]);
  const [team, setTeam] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [replyText, setReplyText] = useState('');
  const [modal, setModal] = useState(null); // 'resolve' | 'reject' | 'transfer' | 'escalate' | 'close' | null

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([getTicket(id), getConversation(id)])
      .then(([t, c]) => {
        setTicket(t.data);
        setConversation(c.data || []);
        markRead(id).catch(() => { /* best-effort - not worth surfacing an error for this */ });
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (ticket?.departmentId) {
      getTeam(ticket.departmentId).then((res) => setTeam(res.data || [])).catch(() => { /* transfer target list just won't show */ });
    }
  }, [ticket?.departmentId]);

  const isRaiser = ticket && myEmployeeId === ticket.raisedById;
  const isAssignee = ticket && myEmployeeId === ticket.assignedToId;
  // A ticket is claimable by any team member of its department while it has
  // no assignee yet (freshly raised, or just transferred/escalated back to
  // OPEN). Don't gate this on isAssignee - assignedToId is null at that
  // point, so isAssignee can never be true for an unclaimed ticket.
  const isTeamMember = ticket && team.some((m) => m.employeeId === myEmployeeId);
  const canClaim = ticket && ticket.status === 'OPEN' && !ticket.assignedToId && isTeamMember;

  async function runAction(promise, successMessage) {
    setError('');
    try {
      await promise;
      setSuccess(successMessage);
      setModal(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleReply(e) {
    e.preventDefault();
    if (!replyText.trim()) return;
    setError('');
    try {
      await reply(id, replyText, null);
      setReplyText('');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <Loading />;
  if (!ticket) return <ErrorBanner message={error || 'Ticket not found'} />;

  const canReply = ticket.status !== 'CLOSED' && (isRaiser || isAssignee);
  const transferTargets = team.filter((m) => m.roleInTeam === 'MEMBER' && m.employeeId !== ticket.assignedToId);

  return (
    <div>
      <div className="page-header">
        <h1>{ticket.ticketNumber}: {ticket.title}</h1>
        <Link to="/tickets" className="btn btn-secondary">Back to Tickets</Link>
      </div>

      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      {ticket.acceptanceBreached && (
        <div className="sla-breach-banner">This ticket was not claimed within its acceptance SLA window.</div>
      )}
      {ticket.resolutionBreached && (
        <div className="sla-breach-banner">This ticket was not resolved within its resolution SLA window.</div>
      )}

      <div className="ticket-meta">
        <span>Department: <strong>{ticket.departmentName}</strong></span>
        <span>Raised by: <strong>{ticket.raisedByName}</strong></span>
        <span>Assigned to: <strong>{ticket.assignedToName || 'Unassigned'}</strong></span>
        <span>Priority: <TicketPriorityBadge priority={ticket.priority} /></span>
        <span>Status: <TicketStatusBadge status={ticket.status} /></span>
        {ticket.escalationLevel > 0 && <span>Escalated ×{ticket.escalationLevel}</span>}
      </div>

      <div className="card-panel">
        <p>{ticket.description}</p>
      </div>

      <div className="ticket-actions">
        {canClaim && (
          <button className="btn btn-primary" onClick={() => runAction(claimTicket(id), 'Ticket claimed')}>Claim</button>
        )}
        {isAssignee && ticket.status === 'IN_PROGRESS' && (
          <>
            <button className="btn btn-primary" onClick={() => setModal('resolve')}>Resolve</button>
            <button className="btn btn-danger" onClick={() => setModal('reject')}>Reject</button>
            <button className="btn btn-secondary" onClick={() => setModal('transfer')}>Transfer</button>
          </>
        )}
        {isRaiser && ['RESOLVED', 'REJECTED'].includes(ticket.status) && (
          <>
            <button className="btn btn-primary" onClick={() => runAction(acceptResolution(id), 'Outcome accepted - ticket closed')}>Accept</button>
            <button className="btn btn-secondary" onClick={() => setModal('escalate')}>Escalate</button>
          </>
        )}
        {isRaiser && ['OPEN', 'IN_PROGRESS'].includes(ticket.status) && (
          <button className="btn btn-secondary" onClick={() => setModal('close')}>Close</button>
        )}
      </div>

      <h2>Conversation</h2>
      <div className="conversation-thread">
        {conversation.length ? conversation.map((entry) => (
          <ConversationEntry key={entry.id} entry={entry} myEmployeeId={myEmployeeId} />
        )) : <p className="empty-row">No activity yet</p>}
      </div>

      {canReply && (
        <form className="reply-box" onSubmit={handleReply}>
          <textarea
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder="Write a reply…"
            rows={2}
            maxLength={2000}
          />
          <button type="submit" className="btn btn-primary">Send</button>
        </form>
      )}

      {modal === 'resolve' && (
        <ActionModal
          title="Resolve Ticket"
          messageLabel="Resolution note"
          onCancel={() => setModal(null)}
          onConfirm={(message) => runAction(resolveTicket(id, message), 'Ticket resolved')}
        />
      )}
      {modal === 'reject' && (
        <ActionModal
          title="Reject Ticket"
          requireMessage
          messageLabel="Reason for rejection"
          onCancel={() => setModal(null)}
          onConfirm={(message) => runAction(rejectTicket(id, message), 'Ticket rejected')}
        />
      )}
      {modal === 'escalate' && (
        <ActionModal
          title="Escalate Ticket"
          messageLabel="Why are you escalating?"
          onCancel={() => setModal(null)}
          onConfirm={(message) => runAction(escalateTicket(id, message), 'Ticket escalated')}
        />
      )}
      {modal === 'close' && (
        <ActionModal
          title="Close Ticket"
          messageLabel="Note (optional)"
          onCancel={() => setModal(null)}
          onConfirm={(message) => runAction(closeTicket(id, message), 'Ticket closed')}
        />
      )}
      {modal === 'transfer' && (
        <TransferModal
          targets={transferTargets}
          onCancel={() => setModal(null)}
          onConfirm={(targetEmployeeId, message) => runAction(transferTicket(id, targetEmployeeId, message), 'Ticket transferred')}
        />
      )}
    </div>
  );
}

function TransferModal({ targets, onCancel, onConfirm }) {
  const [targetEmployeeId, setTargetEmployeeId] = useState(targets[0]?.employeeId || '');
  const [message, setMessage] = useState('');
  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => { e.preventDefault(); onConfirm(Number(targetEmployeeId), message); }}
      >
        <h2>Transfer Ticket</h2>
        <div className="form-grid">
          <label>
            Transfer to
            <select value={targetEmployeeId} onChange={(e) => setTargetEmployeeId(e.target.value)} required>
              <option value="" disabled>Select team member</option>
              {targets.map((m) => <option key={m.employeeId} value={m.employeeId}>{m.employeeName}</option>)}
            </select>
          </label>
          <label>
            Note (optional)
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={4} maxLength={2000} />
          </label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={!targets.length}>Confirm</button>
        </div>
      </form>
    </div>
  );
}
