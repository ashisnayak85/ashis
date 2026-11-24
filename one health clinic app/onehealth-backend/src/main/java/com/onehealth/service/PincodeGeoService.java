package com.onehealth.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves an Indian PIN code to an approximate lat/long centroid, purely to
 * power distance-to-nearest-clinic math for the expansion-opportunity
 * dashboard - NOT for precise navigation.
 *
 * DELIBERATE SCOPE LIMIT: this ships with a small hand-picked table keyed by
 * the first 3 digits of the PIN (India's postal "sorting district" prefix),
 * which gives city/region-level accuracy - good enough to tell "this pincode
 * is ~40km from your nearest branch", not good enough for turn-by-turn
 * directions. Before relying on this for a real expansion decision, replace
 * this table with either (a) the full ~19,000-row government pincode
 * lat/long dataset (available from data.gov.in / India Post), imported into
 * a proper `pincodes` table, or (b) a geocoding API call. This class is
 * intentionally isolated behind one method so swapping the implementation
 * later doesn't touch any calling code.
 */
@Service
public class PincodeGeoService {

    private record LatLng(double lat, double lng) {}

    // First-3-digits prefix -> approximate city centroid. Covers major metro
    // and tier-2 postal circles; extend this table (or replace it per the
    // class javadoc) as your patient base grows into new regions.
    private static final Map<String, LatLng> PREFIX_TO_LATLNG = Map.ofEntries(
            Map.entry("110", new LatLng(28.6139, 77.2090)), // Delhi
            Map.entry("400", new LatLng(19.0760, 72.8777)), // Mumbai
            Map.entry("560", new LatLng(12.9716, 77.5946)), // Bengaluru
            Map.entry("600", new LatLng(13.0827, 80.2707)), // Chennai
            Map.entry("500", new LatLng(17.3850, 78.4867)), // Hyderabad
            Map.entry("700", new LatLng(22.5726, 88.3639)), // Kolkata
            Map.entry("411", new LatLng(18.5204, 73.8567)), // Pune
            Map.entry("380", new LatLng(23.0225, 72.5714)), // Ahmedabad
            Map.entry("302", new LatLng(26.9124, 75.7873)), // Jaipur
            Map.entry("226", new LatLng(26.8467, 80.9462)), // Lucknow
            Map.entry("682", new LatLng(9.9312, 76.2673)),  // Kochi
            Map.entry("641", new LatLng(11.0168, 76.9558))  // Coimbatore
    );

    /**
     * @return resolved [lat, lng], or null if the pincode is missing/unrecognized
     * (in which case distance-based features simply skip that patient rather
     * than guessing).
     */
    public double[] resolve(String pincode) {
        if (pincode == null || pincode.length() < 3) return null;
        LatLng match = PREFIX_TO_LATLNG.get(pincode.substring(0, 3));
        if (match == null) return null;
        return new double[]{match.lat(), match.lng()};
    }

    /** Great-circle distance in km between two lat/long points (Haversine formula). */
    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
