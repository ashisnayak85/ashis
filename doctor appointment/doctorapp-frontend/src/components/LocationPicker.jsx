import { useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from "react-leaflet";
import L from "leaflet";
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";
import "leaflet/dist/leaflet.css";

// Leaflet's default marker icon paths break under most bundlers because they're
// resolved relative to the CSS file, not the JS module - this rewires them to
// the actual bundled asset URLs so the pin actually renders.
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

const DEFAULT_CENTER = [20.5937, 78.9629]; // Geographic center of India - fallback only
const DEFAULT_ZOOM = 5;
const PICKED_ZOOM = 16;

function ClickToPlace({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

function RecenterOnChange({ position }) {
  const map = useMap();
  useEffect(() => {
    if (position) {
      map.setView(position, Math.max(map.getZoom(), PICKED_ZOOM));
    }
  }, [position, map]);
  return null;
}

/**
 * Map-based location picker: search an address (free OpenStreetMap/Nominatim
 * geocoding, no API key), click anywhere on the map, drag the pin, or use the
 * browser's current-location - all three write back to onChange(lat, lng).
 *
 * Note on Nominatim: the public endpoint is rate-limited and meant for light,
 * non-commercial-scale use. Fine for development/small deployments; for
 * production at real volume, swap the search call for a paid geocoder
 * (Google Places, Mapbox, LocationIQ) or a self-hosted Nominatim instance -
 * the map/marker code below doesn't need to change either way.
 */
export default function LocationPicker({ latitude, longitude, onChange }) {
  const hasInitial = latitude != null && longitude != null && latitude !== "" && longitude !== "";
  const [position, setPosition] = useState(
    hasInitial ? [Number(latitude), Number(longitude)] : null
  );
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [results, setResults] = useState([]);
  const debounceRef = useRef(null);

  function place(lat, lng) {
    const rounded = [Number(lat.toFixed(6)), Number(lng.toFixed(6))];
    setPosition(rounded);
    onChange(rounded[0], rounded[1]);
  }

  function handleSearchInput(value) {
    setQuery(value);
    setResults([]);
    setSearchError("");
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (value.trim().length < 3) return;
    debounceRef.current = setTimeout(() => runSearch(value), 500);
  }

  async function runSearch(value) {
    setSearching(true);
    setSearchError("");
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&limit=5&q=${encodeURIComponent(value)}`,
        { headers: { Accept: "application/json" } }
      );
      if (!res.ok) throw new Error("geocode failed");
      const data = await res.json();
      setResults(data);
      if (data.length === 0) setSearchError("No matches for that address. Try a nearby landmark or click the map directly.");
    } catch {
      setSearchError("Couldn't search that address right now. You can still click the map to drop a pin.");
    } finally {
      setSearching(false);
    }
  }

  function pickResult(r) {
    place(parseFloat(r.lat), parseFloat(r.lon));
    setQuery(r.display_name);
    setResults([]);
  }

  function useMyLocation() {
    if (!navigator.geolocation) {
      setSearchError("Location isn't available in this browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => place(pos.coords.latitude, pos.coords.longitude),
      () => setSearchError("Couldn't get your current location.")
    );
  }

  return (
    <div className="location-picker">
      <div className="field" style={{ position: "relative" }}>
        <label htmlFor="lp-search">Search address to place the pin</label>
        <input
          id="lp-search"
          placeholder="Start typing an address or landmark..."
          value={query}
          onChange={(e) => handleSearchInput(e.target.value)}
          autoComplete="off"
        />
        {searching && <div className="location-picker-hint">Searching...</div>}
        {results.length > 0 && (
          <ul className="location-picker-results">
            {results.map((r) => (
              <li key={r.place_id} onClick={() => pickResult(r)}>
                {r.display_name}
              </li>
            ))}
          </ul>
        )}
      </div>

      {searchError && <div className="location-picker-hint location-picker-error">{searchError}</div>}

      <div className="location-picker-map">
        <MapContainer
          center={position || DEFAULT_CENTER}
          zoom={position ? PICKED_ZOOM : DEFAULT_ZOOM}
          style={{ height: "320px", width: "100%", borderRadius: "10px" }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickToPlace onPick={place} />
          <RecenterOnChange position={position} />
          {position && (
            <Marker
              position={position}
              draggable
              eventHandlers={{
                dragend: (e) => {
                  const { lat, lng } = e.target.getLatLng();
                  place(lat, lng);
                },
              }}
            />
          )}
        </MapContainer>
      </div>

      <div className="location-picker-actions">
        <button type="button" className="btn btn-secondary" onClick={useMyLocation}>
          Use my current location
        </button>
        <span className="location-picker-hint">
          {position
            ? `Pin set at ${position[0].toFixed(6)}, ${position[1].toFixed(6)} — drag it or click elsewhere to adjust.`
            : "Search an address, click the map, or use your current location to drop a pin."}
        </span>
      </div>
    </div>
  );
}
