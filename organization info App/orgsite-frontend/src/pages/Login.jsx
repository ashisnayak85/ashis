import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, saveSession } from "../api/auth";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const auth = await login({ email, password });
      saveSession(auth);
      navigate("/admin");
    } catch (err) {
      setError(err.response?.data?.message || "Login failed. Check your credentials.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Log in</h1>
        <p className="auth-subtitle">Manage your business page</p>

        {error && <div className="alert-error">{error}</div>}

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>

        <button className="btn-primary" type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Log in"}
        </button>

        <p className="auth-footer">
          No account yet? <Link to="/register">Create one</Link>
        </p>
        <p className="auth-footer small">
          Demo login: <code>owner@sunriseteahouse.example</code> / <code>Demo@1234</code>
        </p>
      </form>
    </div>
  );
}
