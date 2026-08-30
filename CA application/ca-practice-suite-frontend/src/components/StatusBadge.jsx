const TONE_BY_STATUS = {
  PENDING: 'badge-warning',
  IN_PROGRESS: 'badge-info',
  FILED: 'badge-success',
  OVERDUE: 'badge-danger',
  DRAFT: 'badge-muted',
  SENT: 'badge-info',
  PAID: 'badge-success',
  CANCELLED: 'badge-muted',
};

export default function StatusBadge({ status }) {
  const tone = TONE_BY_STATUS[status] || 'badge-muted';
  return <span className={`badge ${tone}`}>{status?.replace('_', ' ')}</span>;
}
