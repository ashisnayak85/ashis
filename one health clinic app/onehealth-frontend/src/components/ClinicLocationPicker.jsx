import { useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer, Marker, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Leaflet's default marker icon references image files by a relative path that
// breaks under Vite's bundling - point it at the CDN copies instead. Without
// this the marker renders as a broken image icon.
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const DEFAULT_CENTER = [20.5937, 78.9629]; // India, roughly - just a sane default before a location is picked

function ClickToPlaceMarker({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

/**
 * Pulls a lat/lng pair out of text pasted from Google Maps. Handles, in
 * priority order:
 *  1. A raw "lat, lng" pair - what you get from Google Maps' right-click
 *     context menu (the numbers shown at the top, or "What's here?"). This is
 *     the recommended method - it always works, regardless of URL format.
 *  2. The `!3d<lat>!4d<lng>` pattern Google embeds for the actual pinned
 *     place in a full maps.google.com/maps/place/... URL - more accurate than
 *     the `@` viewport-center coordinate below when a specific place is open.
 *  3. The `@<lat>,<lng>` viewport-center pattern present in most Maps URLs.
 *  4. A `?q=<lat>,<lng>` or `&q=<lat>,<lng>` query param.
 *
 * Does NOT handle shortened share links (maps.app.goo.gl/...) - those redirect
 * server-side and the coordinates aren't visible in the short URL itself, so
 * there's nothing to parse client-side. The UI tells people to use the
 * right-click-copy method or the full address-bar URL instead of a share link.
 */
function parseLatLngFromGoogleMapsText(text) {
  if (!text) return null;
  const trimmed = text.trim();

  const rawPair = trimmed.match(/^(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)$/);
  if (rawPair) return { lat: parseFloat(rawPair[1]), lng: parseFloat(rawPair[2]) };

  const place = trimmed.match(/!3d(-?\d{1,3}\.\d+)!4d(-?\d{1,3}\.\d+)/);
  if (place) return { lat: parseFloat(place[1]), lng: parseFloat(place[2]) };

  const viewport = trimmed.match(/@(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)/);
  if (viewport) return { lat: parseFloat(viewport[1]), lng: parseFloat(viewport[2]) };

  const queryParam = trimmed.match(/[?&]q=(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)/);
  if (queryParam) return { lat: parseFloat(queryParam[1]), lng: parseFloat(queryParam[2]) };

  return null;
}

/**
 * Three ways to pin a branch's exact location, roughly in order of how
 * reliable they are for less-common addresses:
 *
 * 1. "Find on Google Maps" - opens Google's actual maps.google.com search
 *    (their full index, not a metered API - this is just the free public
 *    website) in a new tab, pre-filled with whatever's typed in the address
 *    box. No API key/billing needed since we're not embedding their map.
 * 2. Paste back the coordinates (or the page URL) from that tab - parsed
 *    client-side, see parseLatLngFromGoogleMapsText above.
 * 3. OpenStreetMap search (free, built-in, no round-trip) + click/drag on the
 *    embedded map - works well for well-known places, less reliably for
 *    small/rural/less-mapped addresses since it's crowd-sourced data.
 *
 * Whichever method is used, the result always ends up as a marker on the
 * embedded map below, so the branch owner/staff can visually confirm the pin
 * before saving either way.
 */
export default function ClinicLocationPicker({ latitude, longitude, onChange }) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [searchedOnce, setSearchedOnce] = useState(false);
  const [pasteValue, setPasteValue] = useState("");
  const [pasteError, setPasteError] = useState("");
  const debounceRef = useRef(null);

  const position = latitude != null && longitude != null ? [latitude, longitude] : null;

  useEffect(() => () => clearTimeout(debounceRef.current), []);

  function handleQueryChange(e) {
    const value = e.target.value;
    setQuery(value);
    clearTimeout(debounceRef.current);
    if (value.trim().length < 3) {
      setResults([]);
      setSearchedOnce(false);
      return;
    }
    debounceRef.current = setTimeout(() => search(value), 500);
  }

  async function search(value) {
    setSearching(true);
    setSearchError("");
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=5&q=${encodeURIComponent(value)}`
      );
      if (!res.ok) throw new Error("Search failed");
      const data = await res.json();
      setResults(data);
      setSearchedOnce(true);
    } catch {
      setSearchError("Couldn't search for that address right now.");
    } finally {
      setSearching(false);
    }
  }

  function pickResult(result) {
    const lat = parseFloat(result.lat);
    const lon = parseFloat(result.lon);
    onChange({
      latitude: lat,
      longitude: lon,
      address: result.display_name,
      city: result.address?.city || result.address?.town || result.address?.village || "",
      pincode: result.address?.postcode || "",
    });
    setResults([]);
    setQuery(result.display_name);
  }

  function handleMapPick(lat, lng) {
    onChange({ latitude: lat, longitude: lng });
  }

  function openInGoogleMaps() {
    const q = query.trim() || "clinic address";
    window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`, "_blank", "noopener");
  }

  function handleUsePastedLocation() {
    setPasteError("");
    const parsed = parseLatLngFromGoogleMapsText(pasteValue);
    if (!parsed) {
      setPasteError(
        "Couldn't find coordinates in that text. Easiest way: on Google Maps, right-click the exact spot " +
        "(or long-press on mobile) - the coordinates shown at the top of the menu can be tapped to copy. " +
        "Paste just those numbers here. A shortened share link (maps.app.goo.gl/...) won't work - use the " +
        "full address-bar URL instead, or the right-click method."
      );
      return;
    }
    onChange({ latitude: parsed.lat, longitude: parsed.lng });
    setPasteValue("");
  }

  return (
    <div>
      <div className="field">
        <label>Search for the branch's address</label>
        <input
          value={query}
          onChange={handleQueryChange}
          placeholder="Start typing an address..."
        />
        {searching && <div style={{ fontSize: "0.8rem", color: "var(--ink-soft)", marginTop: 4 }}>Searching...</div>}
        {searchError && <div className="form-error">{searchError}</div>}
        {results.length > 0 && (
          <div className="card" style={{ marginTop: 8, padding: 8 }}>
            {results.map((r) => (
              <div
                key={r.place_id}
                onClick={() => pickResult(r)}
                style={{ padding: "8px 6px", cursor: "pointer", borderBottom: "1px solid var(--line)", fontSize: "0.9rem" }}
              >
                {r.display_name}
              </div>
            ))}
          </div>
        )}
      </div>

      {searchedOnce && results.length === 0 && !searching && (
        <div className="card" style={{ padding: 12, marginBottom: 16, background: "#fdf7ec" }}>
          <strong style={{ fontSize: "0.9rem" }}>No matches for that search.</strong>
          <p style={{ fontSize: "0.85rem", color: "var(--ink-soft)", margin: "6px 0 12px" }}>
            This happens for less-common addresses - our free map search doesn't have Google's full coverage.
            Find it on Google Maps instead, then bring the location back here:
          </p>
          <button type="button" className="btn btn-secondary btn-sm" onClick={openInGoogleMaps}>
            Find "{query}" on Google Maps →
          </button>

          <div className="field" style={{ marginTop: 14 }}>
            <label>Paste the coordinates (or the Google Maps link) here</label>
            <input
              value={pasteValue}
              onChange={(e) => setPasteValue(e.target.value)}
              placeholder="e.g. 12.9716, 77.5946"
            />
            <p style={{ fontSize: "0.78rem", color: "var(--ink-soft)", margin: "4px 0" }}>
              On Google Maps: right-click the exact spot (long-press on mobile) → tap the coordinates shown
              at the top of the menu to copy them → paste here.
            </p>
            {pasteError && <div className="form-error">{pasteError}</div>}
            <button type="button" className="btn btn-primary btn-sm" onClick={handleUsePastedLocation} style={{ marginTop: 6 }}>
              Use this location
            </button>
          </div>
        </div>
      )}

      <p style={{ fontSize: "0.85rem", color: "var(--ink-soft)", marginTop: -4, marginBottom: 12 }}>
        Or click directly on the map / drag the marker below to pin the exact spot yourself.
      </p>

      <div style={{ height: 320, borderRadius: 12, overflow: "hidden", border: "1px solid var(--line)" }}>
        <MapContainer
          center={position || DEFAULT_CENTER}
          zoom={position ? 15 : 5}
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickToPlaceMarker onPick={handleMapPick} />
          {position && (
            <Marker
              position={position}
              draggable
              eventHandlers={{
                dragend: (e) => {
                  const { lat, lng } = e.target.getLatLng();
                  handleMapPick(lat, lng);
                },
              }}
            />
          )}
        </MapContainer>
      </div>

      {position && (
        <p style={{ fontSize: "0.8rem", color: "var(--ink-soft)", marginTop: 8 }}>
          Selected: {position[0].toFixed(6)}, {position[1].toFixed(6)}
        </p>
      )}
    </div>
  );
}
