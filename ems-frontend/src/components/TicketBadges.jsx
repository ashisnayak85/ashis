const STATUS_CLASS = {
  OPEN: 'badge-warning',
  IN_PROGRESS: 'badge-info',
  RESOLVED: 'badge-success',
  REJECTED: 'badge-danger',
  CLOSED: 'badge-muted',
};

const PRIORITY_CLASS = {
  HIGH: 'badge-danger',
  MEDIUM: 'badge-warning',
  LOW: 'badge-muted',
};

export function TicketStatusBadge({ status }) {
  return <span className={`badge ${STATUS_CLASS[status] || 'badge-muted'}`}>{status?.replace('_', ' ')}</span>;
}

export function TicketPriorityBadge({ priority }) {
  return <span className={`badge ${PRIORITY_CLASS[priority] || 'badge-muted'}`}>{priority}</span>;
}

// Small dot shown next to a ticket row/title when the viewer has unseen activity.
export function UnreadDot() {
  return <span className="unread-dot" title="Unread activity" />;
}
