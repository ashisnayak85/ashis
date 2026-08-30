import { Link } from "react-router-dom";

/**
 * Public landing page - lists this organization's branches. Doesn't require
 * login (the /patient/clinics endpoint would require PATIENT auth though, so
 * for a real deployment you'd likely expose a small public branch-list
 * endpoint; kept simple here and gated behind login for now - browsing
 * without an account can be added later if the owner wants it).
 */
export default function Home() {
  return (
    <div className="container">
      <div className="hero">
        <h1>Quality care, close to home</h1>
        <p>
          Book an appointment at any of our branches in seconds, or walk in -
          our front desk can always fit you in against real-time availability.
        </p>
        <div style={{ marginTop: 24, display: "flex", gap: 12, justifyContent: "center" }}>
          <Link to="/register" className="btn btn-primary">Book an appointment</Link>
          <Link to="/login" className="btn btn-secondary">Log in</Link>
        </div>
      </div>
    </div>
  );
}
