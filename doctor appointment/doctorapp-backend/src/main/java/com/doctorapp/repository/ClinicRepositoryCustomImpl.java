package com.doctorapp.repository;

import com.doctorapp.dto.NearbyDoctorResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ClinicRepositoryCustomImpl implements ClinicRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    // Roughly 1 degree of latitude/longitude in km, used for the cheap bounding-box
    // pre-filter so the DB doesn't have to compute Haversine for every single row.
    private static final double KM_PER_DEGREE = 111.0;

    @Override
    @SuppressWarnings("unchecked")
    public List<NearbyDoctorResult> findNearbyDoctors(double lat, double lng, double radiusKm, String specialization) {
        double degreeDelta = radiusKm / KM_PER_DEGREE;

        // A doctor shows up at a clinic only through an APPROVED doctor_clinic_associations
        // row - this is what replaced the old clinics.doctor_id direct ownership column,
        // and is what lets one clinic list multiple doctors and one doctor list at
        // multiple clinics.
        StringBuilder sql = new StringBuilder("""
            SELECT
                d.id AS doctor_id,
                d.name AS doctor_name,
                d.qualification AS qualification,
                d.experience_years AS experience_years,
                d.consultation_fee AS consultation_fee,
                d.profile_image_url AS profile_image_url,
                c.id AS clinic_id,
                c.clinic_name AS clinic_name,
                c.address AS address,
                c.latitude AS latitude,
                c.longitude AS longitude,
                (6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
                    COS(RADIANS(c.longitude) - RADIANS(:lng)) +
                    SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
                )) AS distance_km
            FROM doctor_clinic_associations dca
            JOIN doctors d ON d.id = dca.doctor_id
            JOIN clinics c ON c.id = dca.clinic_id
            """);

        if (specialization != null && !specialization.isBlank()) {
            sql.append("""
                JOIN doctor_specializations ds ON ds.doctor_id = d.id
                JOIN specializations s ON s.id = ds.specialization_id AND LOWER(s.name) = LOWER(:specialization)
                """);
        }

        sql.append("""
            WHERE dca.status = 'APPROVED'
              AND d.verified = true AND d.active = true
              AND c.verified = true AND c.active = true
              AND c.latitude BETWEEN :minLat AND :maxLat
              AND c.longitude BETWEEN :minLng AND :maxLng
            HAVING distance_km <= :radiusKm
            ORDER BY distance_km ASC
            """);

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("lat", lat);
        query.setParameter("lng", lng);
        query.setParameter("minLat", lat - degreeDelta);
        query.setParameter("maxLat", lat + degreeDelta);
        query.setParameter("minLng", lng - degreeDelta);
        query.setParameter("maxLng", lng + degreeDelta);
        query.setParameter("radiusKm", radiusKm);
        if (specialization != null && !specialization.isBlank()) {
            query.setParameter("specialization", specialization);
        }

        List<Object[]> rows = query.getResultList();
        List<NearbyDoctorResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(new NearbyDoctorResult(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                row[3] == null ? null : ((Number) row[3]).intValue(),
                row[4] == null ? null : new BigDecimal(row[4].toString()),
                (String) row[5],
                ((Number) row[6]).longValue(),
                (String) row[7],
                (String) row[8],
                ((Number) row[9]).doubleValue(),
                ((Number) row[10]).doubleValue(),
                ((Number) row[11]).doubleValue()
            ));
        }
        return results;
    }
}
