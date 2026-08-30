import { Link } from "react-router-dom";

export default function Home() {
  return (
    <div className="landing-page">
      <div className="landing-content">
        <h1>A simple website for your business</h1>
        <p>
          Add your logo, photos, menu or services, and contact details — get a clean public page
          your customers can visit. No coding required.
        </p>
        <div className="landing-cta">
          <Link className="btn-primary" to="/register">Create your page — free</Link>
          <Link className="btn-secondary" to="/login">Log in</Link>
        </div>
        <p className="landing-demo">
          See it in action: <Link to="/sunrise-tea-house">/sunrise-tea-house</Link> (demo tea shop)
        </p>
      </div>
    </div>
  );
}
