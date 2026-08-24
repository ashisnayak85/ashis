import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerPatient, registerDoctor, registerClinicAdmin } from "../api/auth";
import { useAuth } from "../context/AuthContext";

const SPECIALIZATIONS = [
  "General Physician", "Dentist", "Cardiologist", "Dermatologist",
  "Pediatrician", "Gynecologist", "Orthopedic", "ENT Specialist",
  "Ophthalmologist", "Psychiatrist",
];

export default function Register() {
  const [role, setRole] = useState("PATIENT");
  const [patientForm, setPatientForm] = useState({ name: "", email: "", phone: "", password: "",dob: "",gender: "" });
  const [doctorForm, setDoctorForm] = useState({
    name: "",
    email: "",
    password: "",
    qualification: "",
    experienceYears: "",
    consultationFee: "",
    specializations: [],
    dob:"",
    gender:""
  });
  const [clinicAdminForm, setClinicAdminForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  function updatePatient(field) {
    return (e) => setPatientForm({ ...patientForm, [field]: e.target.value });
  }
  function updateDoctor(field) {
    return (e) => setDoctorForm({ ...doctorForm, [field]: e.target.value });
  }
  function updateClinicAdmin(field) {
    return (e) => setClinicAdminForm({ ...clinicAdminForm, [field]: e.target.value });
  }
  function toggleSpecialization(name) {
    setDoctorForm((f) => {
      const has = f.specializations.includes(name);
      return {
        ...f,
        specializations: has
          ? f.specializations.filter((s) => s !== name)
          : [...f.specializations, name],
      };
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      if (role === "PATIENT") {
        const res = await registerPatient({...patientForm,
          dob:patientForm.dob || null ,
          gender:patientForm.gender || null
      });
        login(res);
        navigate("/");
      } else if (role === "DOCTOR") {
        const res = await registerDoctor({
          ...doctorForm,
          experienceYears: doctorForm.experienceYears ? Number(doctorForm.experienceYears) : null,
          consultationFee: doctorForm.consultationFee ? Number(doctorForm.consultationFee) : null,
          dob:doctorForm.dob || null ,
          gender:doctorForm.gender || null 
        });
        login(res);
        navigate("/doctor");
      } else {
        const res = await registerClinicAdmin(clinicAdminForm);
        login(res);
        navigate("/clinic-admin");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't create your account. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="form-card wide">
      <h2>Create your account</h2>
      <p>Join as a patient to book care, a doctor to manage your practice, or a clinic to manage your doctors.</p>

      <div className="role-tabs">
        <button
          type="button"
          className={`role-tab ${role === "PATIENT" ? "active" : ""}`}
          onClick={() => setRole("PATIENT")}
        >
          I'm a Patient
        </button>
        <button
          type="button"
          className={`role-tab ${role === "DOCTOR" ? "active" : ""}`}
          onClick={() => setRole("DOCTOR")}
        >
          I'm a Doctor
        </button>
        <button
          type="button"
          className={`role-tab ${role === "CLINIC_ADMIN" ? "active" : ""}`}
          onClick={() => setRole("CLINIC_ADMIN")}
        >
          I manage a Clinic
        </button>
      </div>

      {error && <div className="form-error">{error}</div>}

      {role === "PATIENT" && (
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="p-name">Full name</label>
            <input id="p-name" required value={patientForm.name} onChange={updatePatient("name")} />
          </div>
          <div className="field">
            <label htmlFor="p-email">Email</label>
            <input id="p-email" type="email" required value={patientForm.email} onChange={updatePatient("email")} />
          </div>
          <div className="field">
            <label htmlFor="p-phone">Phone</label>
            <input id="p-phone" value={patientForm.phone} onChange={updatePatient("phone")} />
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="p-dob">
                Date Of Birth
              </label>
              <input id="p-dob"  type="date"  max={new Date().toISOString().split("T")[0]} value={patientForm.dob} onChange={updatePatient("dob")} />
            </div>
            <div className="field">
              <label htmlFor="p-gender">
                Gender
              </label>
              <select name="" id="p-gender" value={patientForm.gender} onChange={updatePatient("gender")}>
                <option value="">Prefer not to say</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Others</option>
              </select>
            </div>
          </div>
          <div className="field">
            <label htmlFor="p-password">Password</label>
            <input
              id="p-password"
              type="password"
              required
              value={patientForm.password}
              onChange={updatePatient("password")}
            />
          </div>
          <button className="btn btn-primary btn-block" disabled={loading} type="submit">
            {loading ? "Creating account..." : "Sign up as Patient"}
          </button>
        </form>
      )}

      {role === "DOCTOR" && (
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="field">
              <label htmlFor="d-name">Full name</label>
              <input id="d-name" required value={doctorForm.name} onChange={updateDoctor("name")} />
            </div>
            <div className="field">
              <label htmlFor="d-email">Email</label>
              <input id="d-email" type="email" required value={doctorForm.email} onChange={updateDoctor("email")} />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="d-dob">Date Of Birth</label>
              <input id="d-dob" type="date" max={new Date().toISOString().split("T")[0]} required value={doctorForm.dob} onChange={updateDoctor("dob")} />
            </div>
            <div className="field">
              <label htmlFor="d-gender">Gender</label>
              <select name="" id="d-gender" value={doctorForm.gender} onChange={updateDoctor("gender")}>
                <option value="">Prefer not to say</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Others</option>
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="d-qualification">Qualification</label>
              <input
                id="d-qualification"
                placeholder="e.g. MBBS, MD"
                value={doctorForm.qualification}
                onChange={updateDoctor("qualification")}
              />
            </div>
            <div className="field">
              <label htmlFor="d-experience">Experience (years)</label>
              <input
                id="d-experience"
                type="number"
                min="0"
                value={doctorForm.experienceYears}
                onChange={updateDoctor("experienceYears")}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="d-fee">Consultation fee (Rs.)</label>
              <input
                id="d-fee"
                type="number"
                min="0"
                value={doctorForm.consultationFee}
                onChange={updateDoctor("consultationFee")}
              />
            </div>
            <div className="field">
              <label htmlFor="d-password">Password</label>
              <input
                id="d-password"
                type="password"
                required
                value={doctorForm.password}
                onChange={updateDoctor("password")}
              />
            </div>
          </div>
          <div className="field">
            <label>Specializations</label>
            <div className="chip-group">
              {SPECIALIZATIONS.map((s) => (
                <button
                  type="button"
                  key={s}
                  className={`chip ${doctorForm.specializations.includes(s) ? "chip-selected" : ""}`}
                  onClick={() => toggleSpecialization(s)}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
          <p style={{ fontSize: "0.85rem", marginTop: 4 }}>
            An admin will verify your profile before it appears in patient search. Once verified, you can browse
            clinics and request to join them from your dashboard.
          </p>
          <button className="btn btn-primary btn-block" disabled={loading} type="submit">
            {loading ? "Creating account..." : "Sign up as Doctor"}
          </button>
        </form>
      )}

      {role === "CLINIC_ADMIN" && (
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="field">
              <label htmlFor="ca-name">Your name</label>
              <input id="ca-name" required value={clinicAdminForm.name} onChange={updateClinicAdmin("name")} />
            </div>
            <div className="field">
              <label htmlFor="ca-email">Email</label>
              <input
                id="ca-email"
                type="email"
                required
                value={clinicAdminForm.email}
                onChange={updateClinicAdmin("email")}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="ca-phone">Phone</label>
              <input id="ca-phone" value={clinicAdminForm.phone} onChange={updateClinicAdmin("phone")} />
            </div>
            <div className="field">
              <label htmlFor="ca-password">Password</label>
              <input
                id="ca-password"
                type="password"
                required
                value={clinicAdminForm.password}
                onChange={updateClinicAdmin("password")}
              />
            </div>
          </div>
          <p style={{ fontSize: "0.85rem", marginTop: 4 }}>
            After signing up you can add your clinic's details. An admin will verify the clinic before it can accept
            doctors or appear in patient search.
          </p>
          <button className="btn btn-primary btn-block" disabled={loading} type="submit">
            {loading ? "Creating account..." : "Sign up as Clinic"}
          </button>
        </form>
      )}

      <p style={{ marginTop: 18, textAlign: "center" }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}
