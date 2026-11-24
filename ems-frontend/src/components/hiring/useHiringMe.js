import { useEffect, useState } from 'react';
import { getHiringMe } from '../../api/hiring';

// What the signed-in person can do in hiring: { employeeId, admin, recruiter, staff,
// canRaiseRequisition, pendingApprovals, upcomingInterviews }. null until loaded.
export default function useHiringMe() {
  const [me, setMe] = useState(null);
  useEffect(() => {
    let cancelled = false;
    getHiringMe()
      .then((res) => { if (!cancelled) setMe(res.data); })
      .catch(() => { if (!cancelled) setMe(null); });
    return () => { cancelled = true; };
  }, []);
  return me;
}
