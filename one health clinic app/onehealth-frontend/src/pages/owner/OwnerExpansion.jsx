import { useEffect, useState } from "react";
import { MapContainer, TileLayer, CircleMarker, Marker, Popup, Tooltip } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import DashboardLayout from "../../components/DashboardLayout";
import { getExpansionOpportunities, getClinics } from "../../api/owner";
import { OWNER_NAV_ITEMS } from "./navItems";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const INDIA_CENTER = [20.5937, 78.9629];

/**
 * "Where should we open our next branch" - patient home pincodes plotted as
 * circles (bigger = more patients from there), our existing branches as pins,
 * ranked below by opportunityScore = patientCount x distance from nearest
 * branch. See LocationAnalyticsService on the backend for the scoring logic;
 * this page is purely a view onto GET /owner/expansion-opportunities.
 */
export default function OwnerExpansion() {
  const [opportunities, setOpportunities] = useState([]);
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getExpansionOpportunities(), getClinics()])
      .then(([opps, cl]) => {
        setOpportunities(opps);
        setClinics(cl.filter((c) => c.latitude != null && c.longitude != null));
      })
      .catch(() => setError("Couldn't load expansion analytics."))
      .finally(() => setLoading(false));
  }, []);

  const maxCount = Math.max(1, ...opportunities.map((o) => o.patientCount));
  const center =
    clinics.length > 0
      ? [clinics[0].latitude, clinics[0].longitude]
      : opportunities.length > 0
      ? [opportunities[0].latitude, opportunities[0].longitude]
      : INDIA_CENTER;

  return (
    <DashboardLayout title="Owner Portal" navItems={OWNER_NAV_ITEMS}>
      <div className="section-header">
        <h2>Where to open next</h2>
      </div>
      <p className="text-muted" style={{ marginTop: -8, marginBottom: 20 }}>
        Pincodes ranked by how many patients we already serve there versus how far they currently travel to reach
        a branch. Use this as a starting signal, not a final decision - real estate cost and local competition
        aren't in this data.
      </p>
      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <p className="empty-state">Loading...</p>
      ) : opportunities.length === 0 ? (
        <p className="empty-state">
          Not enough patient location data yet - city/pincode needs to be captured at registration or walk-in
          booking for this to populate.
        </p>
      ) : (
        <>
          <div style={{ height: 420, borderRadius: 12, overflow: "hidden", border: "1px solid var(--line)", marginBottom: 24 }}>
            <MapContainer center={center} zoom={6} style={{ height: "100%", width: "100%" }}>
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {clinics.map((c) => (
                <Marker key={c.id} position={[c.latitude, c.longitude]}>
                  <Popup>
                    <strong>{c.clinicName}</strong>
                    <br />
                    Existing branch
                  </Popup>
                </Marker>
              ))}
              {opportunities.map((o) => (
                <CircleMarker
                  key={o.pincode}
                  center={[o.latitude, o.longitude]}
                  radius={8 + (o.patientCount / maxCount) * 22}
                  pathOptions={{ color: "#c0392b", fillColor: "#e67e22", fillOpacity: 0.55 }}
                >
                  <Tooltip>
                    <strong>{o.city || o.pincode}</strong> ({o.pincode})
                    <br />
                    {o.patientCount} patients • ~{o.avgDistanceFromNearestClinicKm} km from {o.nearestClinicName}
                  </Tooltip>
                </CircleMarker>
              ))}
            </MapContainer>
          </div>

          <div className="card">
            <h3 style={{ marginTop: 0 }}>Top candidate locations</h3>
            <ul className="opportunity-list">
              {opportunities.slice(0, 10).map((o, i) => (
                <li key={o.pincode} className="opportunity-item">
                  <span className="rank">#{i + 1}</span>
                  <div className="meta">
                    <strong>{o.city || "Unknown city"} - {o.pincode}</strong>
                    <span>
                      {o.patientCount} patients • ~{o.avgDistanceFromNearestClinicKm} km from {o.nearestClinicName}
                    </span>
                  </div>
                  <div className="score">
                    <div>{o.opportunityScore}</div>
                    <span className="text-muted" style={{ fontWeight: 400, fontSize: "0.75rem" }}>score</span>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
