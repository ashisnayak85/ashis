package com.doctorapp.repository;

import com.doctorapp.dto.NearbyDoctorResult;

import java.util.List;

public interface ClinicRepositoryCustom {
    /**
     * Finds verified, active doctors within radiusKm of (lat, lng), sorted nearest-first.
     * Uses a bounding-box pre-filter + Haversine formula - no extra geo infrastructure
     * needed at MVP scale. Revisit with MySQL spatial types / Elasticsearch if the
     * clinics table grows past ~50k rows or this query shows up in slow-query logs.
     */
    List<NearbyDoctorResult> findNearbyDoctors(double lat, double lng, double radiusKm, String specialization);
}
