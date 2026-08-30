import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register, saveSession } from "../api/auth";

const CATEGORIES = [
  { value: "RESTAURANT", label: "Restaurant" },
  { value: "CAFE_TEA_SHOP", label: "Cafe / Tea Shop" },
  { value: "SCHOOL", label: "School / Institute" },
  { value: "RETAIL_SHOP", label: "Retail Shop" },
  { value: "SALON_SPA", label: "Salon / Spa" },
  { value: "GYM_FITNESS", label: "Gym / Fitness" },
  { value: "CLINIC", label: "Clinic" },
  { value: "OTHER", label: "Other" },
];

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    organizationName: "",
    category: "RESTAURANT",
    email: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const auth = await register(form);
      saveSession(auth);
      navigate("/admin");
    } catch (err) {
      setError(err.response?.data?.message || "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Create your business page</h1>
        <p className="auth-subtitle">Free, takes about a minute</p>

        {error && <div className="alert-error">{error}</div>}

        <label>
          Business name
          <input
            type="text"
            placeholder="e.g. Sunrise Tea House"
            value={form.organizationName}
            onChange={(e) => update("organizationName", e.target.value)}
            required
          />
        </label>

        <label>
          Category
          <select value={form.category} onChange={(e) => update("category", e.target.value)}>
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Your email
          <input type="email" value={form.email} onChange={(e) => update("email", e.target.value)} required />
        </label>

        <label>
          Password
          <input
            type="password"
            minLength={6}
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
            required
          />
        </label>

        <button className="btn-primary" type="submit" disabled={loading}>
          {loading ? "Creating..." : "Create account"}
        </button>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
