import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { searchCandidates } from '../../api/hiring';
import Pagination from '../../components/Pagination';
import { Loading, ErrorBanner, SuccessBanner } from '../../components/Feedback';
import CandidateForm from '../../components/hiring/CandidateForm';
import { CANDIDATE_SOURCES, money, pretty } from '../../components/hiring/hiringUtils';

export default function Candidates() {
  const navigate = useNavigate();
  const [results, setResults] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [source, setSource] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    searchCandidates({ keyword: search || undefined, source: source || undefined, page, size: 10 })
      .then((res) => setResults(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [search, source, page]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="header-actions">
        <h1>Candidates</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>New candidate</button>
      </div>
      <ErrorBanner message={error} />
      <SuccessBanner message={success} />

      <div className="card-panel">
        <form className="filter-bar" onSubmit={(e) => { e.preventDefault(); setPage(0); setSearch(keyword.trim()); }}>
          <label>
            Search
            <input className="search-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Name, email, mobile, skills, company" />
          </label>
          <label>
            Source
            <select value={source} onChange={(e) => { setPage(0); setSource(e.target.value); }}>
              <option value="">All</option>
              {CANDIDATE_SOURCES.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
            </select>
          </label>
          <button className="btn btn-secondary" type="submit">Search</button>
        </form>

        {loading ? <Loading /> : (
          <>
            <div className="data-table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Name</th><th>Email</th><th>Experience</th><th>Current company</th><th>Expected CTC</th><th>Notice</th><th>Source</th></tr>
                </thead>
                <tbody>
                  {results?.content?.length === 0 && <tr><td colSpan={7} className="empty-row">No candidates found.</td></tr>}
                  {results?.content?.map((c) => (
                    <tr key={c.id} className="clickable-row" onClick={() => navigate(`/hiring/candidates/${c.id}`)}>
                      <td><Link to={`/hiring/candidates/${c.id}`} onClick={(e) => e.stopPropagation()}>{c.firstName} {c.lastName}</Link></td>
                      <td>{c.email}</td>
                      <td>{c.totalExperience ?? '—'} yrs</td>
                      <td>{c.currentCompany || '—'}</td>
                      <td>{money(c.expectedCtc)}</td>
                      <td>{c.noticePeriodDays ?? '—'} d</td>
                      <td>{pretty(c.source)}</td>
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
        <CandidateForm
          onCancel={() => setShowForm(false)}
          onSaved={(saved, warning) => {
            setShowForm(false);
            setSuccess(warning || `Candidate ${saved.firstName} ${saved.lastName || ''} created`);
            load();
            navigate(`/hiring/candidates/${saved.id}`);
          }}
        />
      )}
    </div>
  );
}
