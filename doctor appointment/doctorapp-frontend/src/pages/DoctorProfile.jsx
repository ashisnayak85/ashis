import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getDoctorProfile, getDoctorSlots, getDoctorRatings } from "../api/doctors";
import { bookAppointment } from "../api/appointments";
import { useAuth } from "../context/AuthContext";
import StarRating from "../components/StarRating";

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

// Deep link into Google Maps turn-by-turn directions. Needs no API key/billing -
// it's the same universal link format Maps itself uses, and it opens the Maps
// app on mobile or maps.google.com on desktop.
function directionsUrl(lat, lng) {
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

export default function DoctorProfile() {
  const { doctorId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [clinicId, setClinicId] = useState(null);
  const [date, setDate] = useState(todayISO());
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [error, setError] = useState("");
  const [booking, setBooking] = useState(false);
  const [confirmation, setConfirmation] = useState(null);

  const [ratingSummary, setRatingSummary] = useState(null);
  const [reviewPage, setReviewPage] = useState(0);
  const [loadingReviews, setLoadingReviews] = useState(false);

  useEffect(() => {
    getDoctorProfile(doctorId).then((data) => {
      setProfile(data);
      if (data.clinics?.length) setClinicId(data.clinics[0].id);
    });
    loadRatings(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doctorId]);

  async function loadRatings(page) {
    setLoadingReviews(true);
    try {
      const data = await getDoctorRatings(doctorId, page, 5);
      setRatingSummary((prev) =>
        page === 0 || !prev
          ? data
          : { ...data, reviews: [...prev.reviews, ...data.reviews] }
      );
      setReviewPage(page);
    } catch (err) {
      // Non-critical for the booking flow - fail silently, just show no reviews.
    } finally {
      setLoadingReviews(false);
    }
  }

  useEffect(() => {
    if (clinicId && date) loadSlots();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, date]);

  async function loadSlots() {
    setError("");
    setSelectedSlot(null);
    try {
      const data = await getDoctorSlots(doctorId, clinicId, date);
      setSlots(data);
    } catch (err) {
      setError("Couldn't load availability for that date.");
    }
  }

  async function handleBook() {
    if (!user) {
      navigate("/login");
      return;
    }
    if (!selectedSlot) return;
    setBooking(true);
    setError("");
    try {
      const appointment = await bookAppointment(selectedSlot.slotId);
      setConfirmation(appointment);
      loadSlots();
    } catch (err) {
      setError(err.response?.data?.message || "That slot was just taken. Please pick another.");
      loadSlots();
    } finally {
      setBooking(false);
    }
  }

  if (!profile) return <div className="container"><p>Loading doctor profile...</p></div>;

  if (confirmation) {
    return (
      <div className="container">
        <div className="card" style={{ maxWidth: 480, margin: "40px auto", textAlign: "center" }}>
          <h2>Appointment confirmed</h2>
          <p>
            {confirmation.doctorName} · {confirmation.clinicName}<br />
            {confirmation.appointmentDate} at {confirmation.startTime}
          </p>
          {confirmation.clinicLatitude != null && confirmation.clinicLongitude != null && (
            <a
              className="directions-link"
              style={{ justifyContent: "center" }}
              href={directionsUrl(confirmation.clinicLatitude, confirmation.clinicLongitude)}
              target="_blank"
              rel="noopener noreferrer"
            >
              📍 Get directions to the clinic
            </a>
          )}
          <button className="btn btn-primary" onClick={() => navigate("/my-appointments")}>
            View my appointments
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="card" style={{ marginTop: 24 }}>
        <div style={{ display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}>
          <div className="doctor-avatar" style={{ width: 72, height: 72, fontSize: "1.5rem" }}>
            {profile.name?.[0]}
          </div>
          <div>
            <h2 style={{ marginBottom: 4 }}>{profile.name}</h2>
            <p style={{ margin: 0 }}>{profile.qualification} · {profile.experienceYears ?? 0} yrs experience</p>
            <p style={{ margin: 0 }}>{profile.specializations?.join(", ")}</p>
            {profile.consultationFee != null && <p style={{ margin: 0 }}>₹{profile.consultationFee} consultation fee</p>}
            {profile.ratingCount > 0 && (
              <div className="rating-badge" style={{ marginTop: 6 }}>
                <StarRating value={profile.avgRating} size={16} />
                <span>{profile.avgRating}</span>
                <span className="rating-count">({profile.ratingCount} rating{profile.ratingCount === 1 ? "" : "s"})</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 20 }}>
        <h3>Select clinic & date</h3>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 12 }}>
          <select value={clinicId ?? ""} onChange={(e) => setClinicId(Number(e.target.value))}>
            {profile.clinics.map((c) => (
              <option key={c.id} value={c.id}>{c.clinicName} — {c.address}</option>
            ))}
          </select>
          <input type="date" min={todayISO()} value={date} onChange={(e) => setDate(e.target.value)} />
        </div>

        {(() => {
          const selectedClinic = profile.clinics.find((c) => c.id === clinicId);
          if (!selectedClinic || selectedClinic.latitude == null || selectedClinic.longitude == null) return null;
          return (
            <a
              className="directions-link"
              href={directionsUrl(selectedClinic.latitude, selectedClinic.longitude)}
              target="_blank"
              rel="noopener noreferrer"
            >
              📍 Get directions to {selectedClinic.clinicName}
            </a>
          );
        })()}

        {error && <div className="form-error" style={{ marginTop: 14 }}>{error}</div>}

        <div className="slot-grid">
          {slots.length === 0 && <p>No slots available for this date.</p>}
          {slots.map((s) => {
            const isBooked = s.status !== "AVAILABLE";
            const isSelected = selectedSlot?.slotId === s.slotId;
            return (
              <button
                key={s.slotId}
                type="button"
                disabled={isBooked}
                className={`slot-btn ${isBooked ? "booked" : ""} ${isSelected ? "selected" : ""}`}
                onClick={() => setSelectedSlot(s)}
              >
                {s.startTime}
              </button>
            );
          })}
        </div>

        <button
          className="btn btn-primary"
          style={{ marginTop: 20 }}
          disabled={!selectedSlot || booking}
          onClick={handleBook}
        >
          {booking ? "Booking..." : user ? "Confirm appointment" : "Log in to book"}
        </button>
      </div>

      <div className="card" style={{ marginTop: 20, marginBottom: 40 }}>
        <h3>Ratings & reviews</h3>

        {ratingSummary && ratingSummary.ratingCount > 0 ? (
          <>
            <div className="rating-summary">
              <div className="rating-summary-score">
                <div className="big-number">{ratingSummary.avgRating}</div>
                <StarRating value={ratingSummary.avgRating} size={16} />
                <span className="rating-count">{ratingSummary.ratingCount} rating{ratingSummary.ratingCount === 1 ? "" : "s"}</span>
              </div>
              <div className="rating-distribution">
                {[5, 4, 3, 2, 1].map((star) => {
                  const count = ratingSummary.distribution?.[star] ?? 0;
                  const pct = ratingSummary.ratingCount > 0 ? (count / ratingSummary.ratingCount) * 100 : 0;
                  return (
                    <div className="rating-bar-row" key={star}>
                      <span className="star-label">{star} ★</span>
                      <span className="rating-bar-track">
                        <span className="rating-bar-fill" style={{ width: `${pct}%` }} />
                      </span>
                      <span className="bar-count">{count}</span>
                    </div>
                  );
                })}
              </div>
            </div>

            <div style={{ marginTop: 20 }}>
              {ratingSummary.reviews.map((r) => (
                <div className="review-item" key={r.id}>
                  <div className="review-header">
                    <span className="review-author">{r.patientName}</span>
                    <StarRating value={r.rating} size={14} />
                  </div>
                  <span className="review-date">{new Date(r.createdAt).toLocaleDateString()}</span>
                  {r.reviewText && <p className="review-text">{r.reviewText}</p>}
                </div>
              ))}
            </div>

            {reviewPage + 1 < ratingSummary.totalPages && (
              <button
                className="btn btn-secondary"
                style={{ marginTop: 16 }}
                disabled={loadingReviews}
                onClick={() => loadRatings(reviewPage + 1)}
              >
                {loadingReviews ? "Loading..." : "Load more reviews"}
              </button>
            )}
          </>
        ) : (
          <p>No ratings yet for this doctor.</p>
        )}
      </div>
    </div>
  );
}
