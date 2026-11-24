// Plain helpers and constants for the hiring screens (no components here, so
// react-refresh keeps working on HiringBits.jsx).

export const REQUISITION_STATUSES = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'OPEN', 'ON_HOLD', 'CLOSED', 'REJECTED', 'CANCELLED'];
export const OFFER_STATUSES = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REVOKED', 'CONVERTED'];
export const PIPELINE_STAGES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN'];
export const CANDIDATE_SOURCES = ['REFERRAL', 'JOB_PORTAL', 'CAMPUS', 'AGENCY', 'WALK_IN', 'COMPANY_WEBSITE', 'OTHER'];
export const INTERVIEW_MODES = ['VIDEO', 'IN_PERSON', 'PHONE'];
export const RECOMMENDATIONS = ['STRONG_HIRE', 'HIRE', 'NO_HIRE', 'STRONG_NO_HIRE'];

export const pretty = (value) => (value ? String(value).replaceAll('_', ' ') : '');

export function badgeClass(status) {
  switch (status) {
    case 'APPROVED': case 'OPEN': case 'HIRED': case 'ACCEPTED': case 'CONVERTED':
    case 'COMPLETED': case 'STRONG_HIRE': case 'HIRE':
      return 'badge-success';
    case 'REJECTED': case 'CANCELLED': case 'DECLINED': case 'REVOKED': case 'EXPIRED':
    case 'NO_SHOW': case 'NO_HIRE': case 'STRONG_NO_HIRE':
      return 'badge-danger';
    case 'PENDING_APPROVAL': case 'PENDING': case 'SCREENING': case 'INTERVIEW': case 'SCHEDULED':
    case 'OFFER': case 'SENT':
      return 'badge-warning';
    case 'APPLIED':
      return 'badge-info';
    default: // DRAFT, ON_HOLD, CLOSED, WITHDRAWN, SKIPPED
      return 'badge-muted';
  }
}

export function money(n) {
  if (n === null || n === undefined || n === '') return '—';
  return Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

export function fmtDate(s) {
  if (!s) return '—';
  return new Date(s).toLocaleDateString('en-IN', { dateStyle: 'medium' });
}

export function fmtDateTime(s) {
  if (!s) return '—';
  return new Date(s).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

// <input type="datetime-local"> wants "YYYY-MM-DDTHH:mm" in LOCAL time.
export function toLocalInput(s) {
  if (!s) return '';
  return String(s).slice(0, 16);
}

export function emptyToNull(v) {
  return v === '' || v === undefined ? null : v;
}

export function numOrNull(v) {
  return v === '' || v === undefined || v === null ? null : Number(v);
}

// Opens a blob returned by an authenticated endpoint in a new tab. The tab is opened
// synchronously (inside the click) so popup blockers allow it, then pointed at the blob.
export async function openBlobInNewTab(fetchBlob) {
  const tab = window.open('', '_blank');
  try {
    const blob = await fetchBlob();
    const url = URL.createObjectURL(blob);
    if (tab) {
      tab.location.href = url;
    } else {
      window.location.href = url;
    }
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (err) {
    if (tab) tab.close();
    throw err;
  }
}

// Downloads a blob with a given file name (used for Word resumes, which browsers can't display).
export async function downloadBlob(fetchBlob, filename) {
  const blob = await fetchBlob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'download';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
