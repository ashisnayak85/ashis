import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getHiringDashboard } from '../../api/hiring';
import { Loading, ErrorBanner } from '../../components/Feedback';
import { pretty } from '../../components/hiring/hiringUtils';

function Bars({ data }) {
  const max = Math.max(1, ...Object.values(data));
  return (
    <div>
      {Object.entries(data).map(([key, value]) => (
        <div className="bar-row" key={key}>
          <span>{pretty(key)}</span>
          <div className="bar-track"><div className="bar-fill" style={{ width: `${(value / max) * 100}%` }} /></div>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  );
}

export default function HiringDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getHiringDashboard()
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <div><h1>Hiring Dashboard</h1><ErrorBanner message={error} /></div>;
  if (!data) return <Loading />;

  const stats = [
    ['Open requisitions', data.openRequisitions],
    ['Unfilled positions', data.unfilledPositions],
    ['Awaiting approval', data.requisitionsAwaitingApproval],
    ['Active candidates', data.activeCandidates],
    ['Interviews (next 7 days)', data.interviewsNext7Days],
    ['Hires (last 90 days)', data.hiresLast90Days],
    ['Avg. time to hire (days)', data.averageTimeToHireDays ?? '—'],
  ];

  return (
    <div>
      <h1>Hiring Dashboard</h1>
      <div className="stat-grid">
        {stats.map(([label, value]) => (
          <div className="stat-card" key={label}>
            <div className="stat-label">{label}</div>
            <div className="stat-value">{value}</div>
          </div>
        ))}
      </div>

      <div className="two-col stacked-panel">
        <div className="card-panel">
          <h2>Pipeline (open requisitions)</h2>
          <Bars data={data.pipelineByStage} />
        </div>
        <div className="card-panel">
          <h2>Offers</h2>
          <Bars data={data.offersByStatus} />
          <h2>Candidates by source</h2>
          <Bars data={data.candidatesBySource} />
        </div>
      </div>
      <p className="muted-text">
        <Link to="/hiring/requisitions">Requisitions</Link> · <Link to="/hiring/candidates">Candidates</Link> · <Link to="/hiring/offers">Offers</Link>
      </p>
    </div>
  );
}
