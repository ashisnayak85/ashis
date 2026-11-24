package com.onehealth.service;

import com.onehealth.dto.LocationOpportunityDTO;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.Patient;
import com.onehealth.repository.ClinicRepository;
import com.onehealth.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Answers "where should we open our next branch": groups patients by pincode,
 * finds how far each pincode is from the organization's nearest ACTIVE clinic,
 * and ranks pincodes by opportunityScore = patientCount * avgDistanceKm - see
 * LocationOpportunityDTO javadoc for why that combination (not just distance,
 * not just count) is the useful signal.
 *
 * Deliberately independent of the dashboard's date-range filter: a patient's
 * home location doesn't change day to day, so this looks at ALL patients with
 * a resolvable pincode, not just ones with a visit in the selected range.
 */
@Service
@RequiredArgsConstructor
public class LocationAnalyticsService {

    // Below this many patients, a pincode is noise - not worth surfacing as an
    // expansion candidate even if it happens to be far away.
    private static final int MIN_PATIENTS_TO_SURFACE = 2;

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PincodeGeoService pincodeGeoService;

    @Transactional(readOnly = true)
    public List<LocationOpportunityDTO> getExpansionOpportunities(Long organizationId) {
        List<Clinic> activeClinics = clinicRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .collect(Collectors.toList());

        List<Patient> patients = patientRepository.findByOrganizationId(organizationId);

        Map<String, List<Patient>> byPincode = patients.stream()
                .filter(p -> p.getPincode() != null && !p.getPincode().isBlank())
                .collect(Collectors.groupingBy(Patient::getPincode));

        return byPincode.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_PATIENTS_TO_SURFACE)
                .map(e -> buildOpportunity(e.getKey(), e.getValue(), activeClinics))
                .filter(o -> o != null)
                .sorted((a, b) -> Double.compare(b.getOpportunityScore(), a.getOpportunityScore()))
                .collect(Collectors.toList());
    }

    private LocationOpportunityDTO buildOpportunity(String pincode, List<Patient> patientsHere, List<Clinic> clinics) {
        // Prefer a patient record that already has resolved coordinates; otherwise
        // resolve fresh from the pincode (covers records created before this
        // feature existed, or where resolution failed at registration time).
        Double lat = patientsHere.stream().map(Patient::getLatitude).filter(v -> v != null).findFirst().orElse(null);
        Double lng = patientsHere.stream().map(Patient::getLongitude).filter(v -> v != null).findFirst().orElse(null);
        if (lat == null || lng == null) {
            double[] resolved = pincodeGeoService.resolve(pincode);
            if (resolved == null) return null; // can't place this pincode - skip rather than guess
            lat = resolved[0];
            lng = resolved[1];
        }

        String city = patientsHere.stream().map(Patient::getCity)
                .filter(c -> c != null && !c.isBlank()).findFirst().orElse(null);

        if (clinics.isEmpty()) {
            // No geo-located clinics yet to measure distance against - still
            // surface the pincode (patient count matters on its own), just
            // without a distance figure.
            return LocationOpportunityDTO.builder()
                    .pincode(pincode)
                    .city(city)
                    .patientCount(patientsHere.size())
                    .avgDistanceFromNearestClinicKm(0)
                    .nearestClinicName("(no branch locations set)")
                    .opportunityScore(0)
                    .latitude(lat)
                    .longitude(lng)
                    .build();
        }

        Clinic nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Clinic c : clinics) {
            double d = pincodeGeoService.distanceKm(lat, lng, c.getLatitude(), c.getLongitude());
            if (d < nearestDistance) {
                nearestDistance = d;
                nearest = c;
            }
        }

        double roundedDistance = Math.round(nearestDistance * 10.0) / 10.0;
        double score = Math.round(patientsHere.size() * roundedDistance * 10.0) / 10.0;

        return LocationOpportunityDTO.builder()
                .pincode(pincode)
                .city(city)
                .patientCount(patientsHere.size())
                .avgDistanceFromNearestClinicKm(roundedDistance)
                .nearestClinicName(nearest != null ? nearest.getClinicName() : null)
                .opportunityScore(score)
                .latitude(lat)
                .longitude(lng)
                .build();
    }
}
