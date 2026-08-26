import { useEffect, useState } from "react";
import { getMyAppointments, cancelAppointment, rateAppointment } from "../api/appointments";
import StarRating from "../components/StarRating";

// Deep link into Google Maps turn-by-turn directions. Needs no API key/billing.
function directionsUrl(lat, lng) {
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

export default function MyAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Per-appointment "compose rating" state, keyed by appointment id.
  const [ratingDrafts, setRatingDrafts] = useState({});
  const [submittingId, setSubmittingId] = useState(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getMyAppointments();
      setAppointments(data);
    } catch (err) {
      setError("Couldn't load your appointments.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(id) {
    try {
      await cancelAppointment(id);
      load();
    } catch (err) {
      setError("Couldn't cancel that appointment. Please try again.");
    }
  }

  function startRating(id) {
    setRatingDrafts((prev) => ({ ...prev, [id]: { stars: 0, text: "" } }));
  }

  function updateDraft(id, patch) {
    setRatingDrafts((prev) => ({ ...prev, [id]: { ...prev[id], ...patch } }));
  }

  async function submitRating(id) {
    const draft = ratingDrafts[id];
    if (!draft?.stars) {
      setError("Please pick a star rating before submitting.");
      return;
    }
    setSubmittingId(id);
    setError("");
    try {
      await rateAppointment(id, draft.stars, draft.text || undefined);
      setRatingDrafts((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      load();
    } catch (err) {
      setError(err.response?.data?.message || "Couldn't submit your rating. Please try again.");
    } finally {
      setSubmittingId(null);
    }
  }

  return (
    <div className="container" style={{ paddingTop: 32, paddingBottom: 40 }}>
      <h2>My appointments</h2>
      {error && <div className="form-error">{error}</div>}
      {loading && <p>Loading...</p>}
      {!loading && appointments.length === 0 && (
        <div className="empty-state">You haven't booked any appointments yet.</div>
      )}
      {appointments.map((a) => {
        const draft = ratingDrafts[a.id];
        const showRatePrompt = a.status === "COMPLETED" && !a.rated;
        return (
          <div className="card" key={a.id} style={{ marginBottom: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", flexWrap: "wrap", gap: 10 }}>
              <div>
                <h3 style={{ marginBottom: 4 }}>{a.doctorName}</h3>
                <p style={{ margin: 0 }}>{a.clinicName} — {a.clinicAddress}</p>
                <p style={{ margin: 0 }}>{a.appointmentDate} · {a.startTime}–{a.endTime}</p>
                {a.status === "BOOKED" && a.clinicLatitude != null && a.clinicLongitude != null && (
                  <a
                    className="directions-link"
                    href={directionsUrl(a.clinicLatitude, a.clinicLongitude)}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    📍 Get directions
                  </a>
                )}
              </div>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
                <span className={`status-pill status-${a.status}`}>{a.status}</span>
                {a.status === "BOOKED" && (
                  <button className="btn btn-secondary" onClick={() => handleCancel(a.id)}>
                    Cancel
                  </button>
                )}
                {a.status === "COMPLETED" && a.rated && (
                  <span className="rated-badge">✓ You rated this visit</span>
                )}
                {showRatePrompt && !draft && (
                  <button className="btn btn-secondary" onClick={() => startRating(a.id)}>
                    Rate your visit
                  </button>
                )}
              </div>
            </div>

            {showRatePrompt && draft && (
              <div className="rate-visit-card">
                <p style={{ margin: "0 0 6px", fontWeight: 600, color: "var(--ink)" }}>
                  How was your visit with {a.doctorName}?
                </p>
                <StarRating value={draft.stars} onChange={(n) => updateDraft(a.id, { stars: n })} readOnly={false} size={26} />
                <textarea
                  placeholder="Share details of your experience (optional)"
                  value={draft.text}
                  maxLength={1000}
                  onChange={(e) => updateDraft(a.id, { text: e.target.value })}
                />
                <div style={{ display: "flex", gap: 10, marginTop: 10 }}>
                  <button
                    className="btn btn-primary"
                    disabled={submittingId === a.id}
                    onClick={() => submitRating(a.id)}
                  >
                    {submittingId === a.id ? "Submitting..." : "Submit rating"}
                  </button>
                  <button
                    className="btn btn-secondary"
                    onClick={() =>
                      setRatingDrafts((prev) => {
                        const next = { ...prev };
                        delete next[a.id];
                        return next;
                      })
                    }
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
