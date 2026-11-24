package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * One pincode's worth of "should we open a branch here?" signal - how many of
 * our patients live there, how far the nearest existing clinic is, and a
 * simple opportunityScore (patientCount * avgDistanceKm) that surfaces areas
 * that are BOTH far away AND already sending us a meaningful number of
 * patients despite the distance. Sorted descending by opportunityScore on the
 * dashboard, this ranks new-branch candidates.
 */
@Getter
@Builder
public class LocationOpportunityDTO {
    private String pincode;
    private String city;
    private long patientCount;
    private double avgDistanceFromNearestClinicKm;
    private String nearestClinicName;
    private double opportunityScore;
    private Double latitude;
    private Double longitude;
}
