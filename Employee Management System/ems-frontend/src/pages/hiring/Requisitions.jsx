import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getHiringLookups, searchRequisitions } from '../../api/hiring';
import Pagination from '../../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import { StatusBadge } from '../../components/hiring/HiringBits';
import RequisitionForm from '../../components/hiring/RequisitionForm';
import useHiringMe from '../../components/hiring/useHiringMe';
import { REQUISITION_STATUSES, pretty } from '../../components/hiring/hiringUtils';

export default function Requisitions() {
  const navigate = useNavigate();
  const me = useHiringMe();
  const [results, setResults] = useState(null);
  const [lookups, setLookups] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    searchRequisitions({ keyword: search || undefined, status: status || undefined, page, size: 10 })
      .then((res) => setResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [search, status, page]);

  useEffect(() => { load(); }, [load]);

  function openForm() {
    setError('');
    getHiringLookups()
      .then((res) => { setLookups(res.data); setShowForm(true); })
      .catch((err) => setError(err.message));
  }

  return (
    <div>
      <div className="header-actions">
        <h1>Job Requisitions</h1>
        {me?.canRaiseRequisition && <button className="btn btn-primary" onClick={openForm}>New requisition</button>}
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="card-panel">
        <form className="filter-bar" onSubmit={(e) => { e.preventDefault(); setPage(0); setSearch(keyword.trim()); }}>
          <label>
            Search
            <input className="search-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Title or REQ code" />
          </label>
          <label>
            Status
            <select value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
              <option value="">All</option>
              {REQUISITION_STATUSES.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
            </select>
          </label>
          <button className="btn btn-secondary" type="submit">Search</button>
        </form>

        {loading ? <Loading /> : (
          <>
            <div className="data-table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Code</th><th>Title</th><th>Department</th><th>Openings</th><th>Hiring manager</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {results?.content?.length === 0 && (
                    <tr><td colSpan={6} className="empty-row">No requisitions found.</td></tr>
                  )}
                  {results?.content?.map((r) => (
                    <tr key={r.id} className="clickable-row" onClick={() => navigate(`/hiring/requisitions/${r.id}`)}>
                      <td><Link to={`/hiring/requisitions/${r.id}`} onClick={(e) => e.stopPropagation()}>{r.requisitionCode}</Link></td>
                      <td>{r.title}</td>
                      <td>{r.departmentName}</td>
                      <td>{r.filledCount}/{r.openings} filled</td>
                      <td>{r.hiringManagerName}</td>
                      <td><StatusBadge status={r.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {results && <Pagination {...results} onChange={setPage} />}
          </>
        )}
      </div>

      {showForm && (
        <RequisitionForm
          lookups={lookups}
          onCancel={() => setShowForm(false)}
          onSaved={(saved) => {
            setShowForm(false);
            setSuccess('Requisition created as a draft. Open it to submit for approval.');
            navigate(`/hiring/requisitions/${saved.id}`);
          }}
        />
      )}
    </div>
  );
}
