import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { findNearbyDoctors } from "../api/doctors";

const SPECIALIZATIONS = [
  "General Physician", "Dentist", "Cardiologist", "Dermatologist",
  "Pediatrician", "Gynecologist", "Orthopedic", "ENT Specialist",
  "Ophthalmologist", "Psychiatrist",
];

export default function Home() {
  const [coords, setCoords] = useState(null);
  const [locationError, setLocationError] = useState("");
  const [specialization, setSpecialization] = useState("");
  const [radiusKm, setRadiusKm] = useState(5);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchError, setSearchError] = useState("");

  useEffect(() => {
    if (!navigator.geolocation) {
      setLocationError("Your browser doesn't support location detection. Enter your location manually below.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => setLocationError("Location access was denied. Enter your coordinates manually to search.")
    );
  }, []);

  useEffect(() => {
    if (coords) runSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [coords]);

  async function runSearch(e) {
    if (e) e.preventDefault();
    if (!coords) {
      setSearchError("We need a location to search. Allow location access or enter coordinates.");
      return;
    }
    setLoading(true);
    setSearchError("");
    try {
      const results = await findNearbyDoctors({ ...coords, radiusKm, specialization });
      setDoctors(results);
    } catch (err) {
      setSearchError("Couldn't load nearby doctors. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <div className="hero">
        <h1>Find a trusted doctor near you</h1>
        <p>See real availability and book a slot instantly — no phone calls, no waiting rooms.</p>

        <form className="search-bar" onSubmit={runSearch}>
          <select value={specialization} onChange={(e) => setSpecialization(e.target.value)}>
            <option value="">Any specialty</option>
            {SPECIALIZATIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <select value={radiusKm} onChange={(e) => setRadiusKm(Number(e.target.value))}>
            {[1, 3, 5, 10, 20].map((r) => (
              <option key={r} value={r}>Within {r} km</option>
            ))}
          </select>
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? "Searching..." : "Search"}
          </button>
        </form>

        {locationError && (
          <p style={{ marginTop: 14, color: "var(--danger)" }}>{locationError}</p>
        )}
        {searchError && (
          <p style={{ marginTop: 14, color: "var(--danger)" }}>{searchError}</p>
        )}
      </div>

      <div style={{ marginTop: 12 }}>
        {doctors.length === 0 && !loading && coords && !searchError && (
          <div className="empty-state">
            No verified doctors found nearby yet. Try a wider radius or a different specialty.
          </div>
        )}

        {doctors.map((doc) => (
          <div className="doctor-card card" key={`${doc.doctorId}-${doc.clinicId}`}>
            <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
              <div className="doctor-avatar">{doc.doctorName?.[0] ?? "D"}</div>
              <div>
                <h3 style={{ marginBottom: 2 }}>{doc.doctorName}</h3>
                <p style={{ margin: 0 }}>{doc.qualification} · {doc.experienceYears ?? 0} yrs experience</p>
                <p style={{ margin: 0 }}>{doc.clinicName} — {doc.address}</p>
              </div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
              <span className="distance-badge">{doc.distanceKm?.toFixed(1)} km away</span>
              {doc.consultationFee != null && <span>₹{doc.consultationFee} consultation</span>}
              <Link to={`/doctors/${doc.doctorId}`} className="btn btn-secondary">
                View & book
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
