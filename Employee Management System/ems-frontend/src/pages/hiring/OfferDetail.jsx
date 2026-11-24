import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getOffer, getHiringLookups, submitOffer, approveOffer, rejectOffer, sendOffer, acceptOffer,
  declineOffer, revokeOffer, convertOffer, fetchOfferLetterBlob,
} from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge, ApprovalTimeline, ReasonModal, EmployeePicker } from '../../components/hiring/HiringBits';
import OfferForm from '../../components/hiring/OfferForm';
import useHiringMe from '../../components/hiring/useHiringMe';
import { fmtDate, fmtDateTime, money, openBlobInNewTab } from '../../components/hiring/hiringUtils';

// Turn an accepted offer into an Employee. Everything is optional - blanks fall back to the
// candidate / offer / requisition data, and the employee code is generated.
function ConvertModal({ offer, onDone, onCancel }) {
  const [form, setForm] = useState({
    employeeCode: '',
    firstName: '',
    lastName: '',
    dateOfJoining: offer.joiningDate || '',
    dateOfBirth: '',
    gender: '',
    salary: offer.annualCtc ? (Number(offer.annualCtc) / 12).toFixed(2) : '',
  });
  const [manager, setManager] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [errors, setErrors] = useState([]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setErrors([]);
    const body = {
      employeeCode: form.employeeCode.trim().toUpperCase() || null,
      firstName: form.firstName.trim() || null,
      lastName: form.lastName.trim() || null,
      dateOfJoining: form.dateOfJoining || null,
      dateOfBirth: form.dateOfBirth || null,
      gender: form.gender || null,
      salary: form.salary === '' ? null : Number(form.salary),
      reportingManagerId: manager?.id ?? null,
    };
    try {
      const res = await convertOffer(offer.id, body);
      onDone(res.data);
    } catch (err) {
      setError(err.message);
      setErrors(err.errors || []);
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h2>Convert {offer.candidateName} to employee</h2>
        <p className="hint-text">
          Creates the employee record through the normal employee creation rules. Leave a field blank to use the default
          (name from the candidate, reporting manager = hiring manager, department / type / location from the requisition).
          A login is still created separately under Users.
        </p>
        <ErrorBanner message={error} errors={errors} />
        <div className="form-grid">
          <label>
            Employee code (blank = auto)
            <input value={form.employeeCode} onChange={set('employeeCode')} maxLength={20} placeholder="e.g. EMP012" />
          </label>
          <label>
            Date of joining
            <input type="date" value={form.dateOfJoining} onChange={set('dateOfJoining')} required />
          </label>
          <label>
            First name override
            <input value={form.firstName} onChange={set('firstName')} maxLength={50} />
          </label>
          <label>
            Last name override
            <input value={form.lastName} onChange={set('lastName')} maxLength={50} />
          </label>
          <label>
            Date of birth
            <input type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
          </label>
          <label>
            Gender
            <select value={form.gender} onChange={set('gender')}>
              <option value="">—</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </label>
          <label>
            Salary to record on the employee
            <input type="number" min={0} step="0.01" value={form.salary} onChange={set('salary')} />
          </label>
          <EmployeePicker label="Reporting manager (default: hiring manager)" value={manager} onChange={setManager} />
        </div>
        <p className="hint-text">Salary is prefilled as annual CTC ÷ 12. Change it to match how your payroll stores the employee salary.</p>
        <div className="modal-actions">
          <button type="button" className="btn btn-link" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Converting…' : 'Convert to employee'}</button>
        </div>
      </form>
    </div>
  );
}

export default function OfferDetail() {
  const { id } = useParams();
  const me = useHiringMe();
  const [offer, setOffer] = useState(null);
  const [lookups, setLookups] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [converted, setConverted] = useState(null);
  const [modal, setModal] = useState(null); // 'edit' | 'reject' | 'decline' | 'revoke' | 'convert'

  const load = useCallback(() => {
    getOffer(id)
      .then((res) => setOffer(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (me?.staff) getHiringLookups().then((res) => setLookups(res.data)).catch(() => {});
  }, [me]);

  async function run(action, okMessage) {
    setError('');
    setSuccess('');
    try {
      const res = await action();
      setOffer(res.data);
      setSuccess(okMessage);
    } catch (err) {
      setError(err.message);
    }
  }

  async function openLetter() {
    setError('');
    try {
      await openBlobInNewTab(() => fetchOfferLetterBlob(id));
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <Loading />;
  if (!offer) return <div><ErrorBanner message={error || 'Offer not found'} /></div>;

  const staff = !!me?.staff;
  const st = offer.status;
  const open = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED'].includes(st);
  const letterReady = ['APPROVED', 'SENT', 'ACCEPTED', 'CONVERTED'].includes(st);

  return (
    <div>
      <p>{staff ? <Link to="/hiring/offers">← Offers</Link> : <Link to="/hiring/approvals">← Approvals</Link>}</p>
      <div className="header-actions">
        <h1>Offer · {offer.candidateName}</h1>
        <StatusBadge status={st} />
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />
      {converted && (
        <div className="banner banner-success">
          Converted. Employee code <strong>{converted.employeeCode}</strong> created. Create their login under Users, and complete the
          rest of their profile under Employees.
        </div>
      )}

      <div className="card-panel">
        <div className="inline-actions">
          {offer.viewerCanApprove && (
            <>
              <button className="btn btn-primary" onClick={() => run(() => approveOffer(id, ''), 'Offer approved')}>Approve</button>
              <button className="btn btn-danger" onClick={() => setModal('reject')}>Reject</button>
            </>
          )}
          {staff && st === 'DRAFT' && (
            <>
              <button className="btn btn-secondary" onClick={() => setModal('edit')}>Edit</button>
              <button className="btn btn-primary" onClick={() => run(() => submitOffer(id), 'Offer submitted for approval')}>Submit for approval</button>
            </>
          )}
          {staff && st === 'APPROVED' && (
            <button className="btn btn-primary" onClick={() => run(() => sendOffer(id), 'Offer sent to the candidate')}>Send to candidate</button>
          )}
          {staff && st === 'SENT' && (
            <>
              <button className="btn btn-primary" onClick={() => run(() => acceptOffer(id), 'Marked as accepted')}>Candidate accepted</button>
              <button className="btn btn-secondary" onClick={() => setModal('decline')}>Candidate declined</button>
            </>
          )}
          {staff && st === 'ACCEPTED' && <button className="btn btn-primary" onClick={() => setModal('convert')}>Convert to employee</button>}
          {staff && letterReady && <button className="btn btn-secondary" onClick={openLetter}>Offer letter</button>}
          {staff && open && <button className="btn btn-danger" onClick={() => setModal('revoke')}>Revoke offer</button>}
        </div>
        {st === 'DRAFT' && offer.approvals?.some((a) => a.status === 'REJECTED') && (
          <p className="hint-text">This offer was rejected during approval and returned to draft. Edit it and submit again.</p>
        )}
      </div>

      <div className="two-col stacked-panel">
        <div className="card-panel">
          <h2>Offer details</h2>
          <dl className="kv-grid">
            <dt>Requisition</dt>
            <dd>{staff ? <Link to={`/hiring/requisitions/${offer.requisitionId}`}>{offer.requisitionCode}</Link> : offer.requisitionCode} · {offer.requisitionTitle}</dd>
            <dt>Designation</dt><dd>{offer.designationName}</dd>
            <dt>Annual CTC</dt>
            <dd>{money(offer.annualCtc)} {offer.aboveBudget && <span className="badge badge-warning">above budget</span>}</dd>
            <dt>Requisition budget</dt><dd>{money(offer.requisitionBudgetMin)} – {money(offer.requisitionBudgetMax)}</dd>
            {offer.budgetExceptionReason && (<><dt>Budget exception</dt><dd>{offer.budgetExceptionReason}</dd></>)}
            <dt>Joining date</dt><dd>{fmtDate(offer.joiningDate)}</dd>
            <dt>Valid until</dt><dd>{fmtDate(offer.expiryDate)}</dd>
            {offer.remarks && (<><dt>Remarks</dt><dd>{offer.remarks}</dd></>)}
            {offer.sentAt && (<><dt>Sent</dt><dd>{fmtDateTime(offer.sentAt)}</dd></>)}
            {offer.respondedAt && (<><dt>Candidate responded</dt><dd>{fmtDateTime(offer.respondedAt)}</dd></>)}
            {offer.closingReason && (<><dt>Closing reason</dt><dd>{offer.closingReason}</dd></>)}
            {offer.convertedEmployeeId && (<><dt>Employee record</dt><dd>#{offer.convertedEmployeeId}</dd></>)}
          </dl>
          {staff && <p><Link to={`/hiring/applications/${offer.applicationId}`}>Open application</Link></p>}
        </div>
        <div className="card-panel">
          <h2>Approvals</h2>
          <ApprovalTimeline approvals={offer.approvals} />
        </div>
      </div>

      {modal === 'edit' && (
        <OfferForm
          initial={offer}
          lookups={lookups}
          onCancel={() => setModal(null)}
          onSaved={(saved) => { setOffer(saved); setModal(null); setSuccess('Offer updated'); }}
        />
      )}
      {modal === 'reject' && (
        <ReasonModal title="Reject offer" label="Reason (required)" required confirmLabel="Reject"
          onCancel={() => setModal(null)}
          onSubmit={(text) => rejectOffer(id, text).then((res) => { setOffer(res.data); setSuccess('Offer rejected and returned to draft'); setModal(null); })} />
      )}
      {modal === 'decline' && (
        <ReasonModal title="Candidate declined" label="Reason (optional)" confirmLabel="Record decline"
          onCancel={() => setModal(null)}
          onSubmit={(text) => declineOffer(id, text).then((res) => { setOffer(res.data); setSuccess('Recorded as declined'); setModal(null); })} />
      )}
      {modal === 'revoke' && (
        <ReasonModal title="Revoke offer" label="Reason (required)" required confirmLabel="Revoke"
          onCancel={() => setModal(null)}
          onSubmit={(text) => revokeOffer(id, text).then((res) => { setOffer(res.data); setSuccess('Offer revoked'); setModal(null); })} />
      )}
      {modal === 'convert' && (
        <ConvertModal
          offer={offer}
          onCancel={() => setModal(null)}
          onDone={(employee) => { setModal(null); setConverted(employee); load(); }}
        />
      )}
    </div>
  );
}
