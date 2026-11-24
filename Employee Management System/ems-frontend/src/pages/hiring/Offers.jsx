import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { searchOffers } from '../../api/hiring';
import Pagination from '../../components/Pagination';
import { Loading, ErrorBanner } from '../../components/Feedback';
import { StatusBadge } from '../../components/hiring/HiringBits';
import { OFFER_STATUSES, fmtDate, money, pretty } from '../../components/hiring/hiringUtils';

export default function Offers() {
  const navigate = useNavigate();
  const [results, setResults] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    searchOffers({ keyword: search || undefined, status: status || undefined, page, size: 10 })
      .then((res) => setResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [search, status, page]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <h1>Offers</h1>
      <ErrorBanner message={error} />
      <div className="card-panel">
        <form className="filter-bar" onSubmit={(e) => { e.preventDefault(); setPage(0); setSearch(keyword.trim()); }}>
          <label>
            Search
            <input className="search-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Candidate or requisition title" />
          </label>
          <label>
            Status
            <select value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
              <option value="">All</option>
              {OFFER_STATUSES.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
            </select>
          </label>
          <button className="btn btn-secondary" type="submit">Search</button>
        </form>
        {loading ? <Loading /> : (
          <>
            <div className="data-table-scroll">
              <table className="data-table">
                <thead><tr><th>Candidate</th><th>Requisition</th><th>Designation</th><th>Annual CTC</th><th>Joining</th><th>Valid until</th><th>Status</th></tr></thead>
                <tbody>
                  {results?.content?.length === 0 && <tr><td colSpan={7} className="empty-row">No offers found.</td></tr>}
                  {results?.content?.map((o) => (
                    <tr key={o.id} className="clickable-row" onClick={() => navigate(`/hiring/offers/${o.id}`)}>
                      <td><Link to={`/hiring/offers/${o.id}`} onClick={(e) => e.stopPropagation()}>{o.candidateName}</Link></td>
                      <td>{o.requisitionCode} · {o.requisitionTitle}</td>
                      <td>{o.designationName}</td>
                      <td>{money(o.annualCtc)}</td>
                      <td>{fmtDate(o.joiningDate)}</td>
                      <td>{fmtDate(o.expiryDate)}</td>
                      <td><StatusBadge status={o.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {results && <Pagination {...results} onChange={setPage} />}
          </>
        )}
      </div>
    </div>
  );
}
