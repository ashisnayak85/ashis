import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login as loginApi } from "../api/auth";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await loginApi({ email, password });
      login(res);
      if (res.role === "OWNER") navigate("/owner");
      else if (res.role === "CLINIC_ADMIN") navigate("/clinic-admin");
      else if (res.role === "DOCTOR") navigate("/doctor");
      else navigate("/my-appointments");
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't log you in. Check your details and try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="form-card">
      <h2>Welcome back</h2>
      <p>Patients, doctors, clinic staff, and the clinic owner all log in here.</p>
      {error && <div className="form-error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn btn-primary btn-block" disabled={loading} type="submit">
          {loading ? "Logging in..." : "Log in"}
        </button>
      </form>
      <p style={{ marginTop: 18, textAlign: "center" }}>
        New here? <Link to="/register">Create an account</Link>
      </p>
    </div>
  );
}
