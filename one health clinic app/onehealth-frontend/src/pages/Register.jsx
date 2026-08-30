import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerPatient } from "../api/auth";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  function update(field) {
    return (e) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await registerPatient(form);
      login(res);
      navigate("/my-appointments");
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't create your account. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="form-card">
      <h2>Create your account</h2>
      <p>Book appointments at any of our branches.</p>
      {error && <div className="form-error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="name">Full name</label>
          <input id="name" required value={form.name} onChange={update("name")} />
        </div>
        <div className="field">
          <label htmlFor="phone">Phone</label>
          <input id="phone" required value={form.phone} onChange={update("phone")} />
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={form.email} onChange={update("email")} />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" required value={form.password} onChange={update("password")} />
        </div>
        <button className="btn btn-primary btn-block" disabled={loading} type="submit">
          {loading ? "Creating account..." : "Sign up"}
        </button>
      </form>
      <p style={{ marginTop: 18, textAlign: "center" }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}
