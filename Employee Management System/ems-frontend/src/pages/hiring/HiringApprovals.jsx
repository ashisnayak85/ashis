import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getPendingRequisitionApprovals, getPendingOfferApprovals, approveRequisition, rejectRequisition,
  approveOffer, rejectOffer,
} from '../../api/hiring';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { ReasonModal } from '../../components/hiring/HiringBits';
import { fmtDate, money } from '../../components/hiring/hiringUtils';

export default function HiringApprovals() {
  const [reqs, setReqs] = useState(null);
  const [offers, setOffers] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [rejecting, setRejecting] = useState(null); // { kind: 'req' | 'offer', id }

  const load = useCallback(() => {
    getPendingRequisitionApprovals().then((res) => setReqs(res.data || [])).catch((err) => setError(err.message));
    getPendingOfferApprovals().then((res) => setOffers(res.data || [])).catch((err) => setError(err.message));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function approve(kind, id) {
    setError('');
    setSuccess('');
    try {
      await (kind === 'req' ? approveRequisition(id, '') : approveOffer(id, ''));
      setSuccess('Approved');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  if (!reqs || !offers) return <div><h1>Hiring Approvals</h1><ErrorBanner message={error} />{!error && <Loading />}</div>;

  return (
    <div>
      <h1>Hiring Approvals</h1>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="card-panel">
        <h2>Requisitions awaiting my approval</h2>
        {reqs.length === 0 ? <p className="empty-row">Nothing waiting.</p> : (
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Code</th><th>Title</th><th>Department</th><th>Openings</th><th>Requested by</th><th></th></tr></thead>
              <tbody>
                {reqs.map((r) => (
                  <tr key={r.id}>
                    <td><Link to={`/hiring/requisitions/${r.id}`}>{r.requisitionCode}</Link></td>
                    <td>{r.title}</td>
                    <td>{r.departmentName}</td>
                    <td>{r.openings}</td>
                    <td>{r.requestedByName}</td>
                    <td>
                      <div className="row-actions">
                        <button className="btn btn-primary" onClick={() => approve('req', r.id)}>Approve</button>
                        <button className="btn btn-danger" onClick={() => setRejecting({ kind: 'req', id: r.id })}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card-panel stacked-panel">
        <h2>Offers awaiting my approval</h2>
        {offers.length === 0 ? <p className="empty-row">Nothing waiting.</p> : (
          <div className="data-table-scroll">
            <table className="data-table">
              <thead><tr><th>Candidate</th><th>Position</th><th>Annual CTC</th><th>Joining</th><th></th></tr></thead>
              <tbody>
                {offers.map((o) => (
                  <tr key={o.id}>
                    <td><Link to={`/hiring/offers/${o.id}`}>{o.candidateName}</Link></td>
                    <td>{o.designationName} <span className="muted-text">({o.requisitionCode})</span></td>
                    <td>{money(o.annualCtc)}{o.aboveBudget && <span className="badge badge-warning">above budget</span>}</td>
                    <td>{fmtDate(o.joiningDate)}</td>
                    <td>
                      <div className="row-actions">
                        <button className="btn btn-primary" onClick={() => approve('offer', o.id)}>Approve</button>
                        <button className="btn btn-danger" onClick={() => setRejecting({ kind: 'offer', id: o.id })}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {rejecting && (
        <ReasonModal title="Reject" label="Reason (required)" required confirmLabel="Reject"
          onCancel={() => setRejecting(null)}
          onSubmit={(text) => (rejecting.kind === 'req' ? rejectRequisition(rejecting.id, text) : rejectOffer(rejecting.id, text))
            .then(() => { setRejecting(null); setSuccess('Rejected'); load(); })} />
      )}
    </div>
  );
}
