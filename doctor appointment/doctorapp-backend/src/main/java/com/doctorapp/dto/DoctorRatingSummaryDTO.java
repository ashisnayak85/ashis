package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Everything the doctor-profile "Ratings & Reviews" section needs in one call:
 * the headline number, the 1-5 star distribution (for the breakdown bars), and
 * one page of individual reviews.
 */
@Getter
@Builder
@AllArgsConstructor
public class DoctorRatingSummaryDTO {
    private Double avgRating;
    private long ratingCount;
    /** Always has keys 5,4,3,2,1 in that order, 0 where there are no ratings at that star. */
    private Map<Integer, Long> distribution;
    private List<RatingDTO> reviews;
    private int page;
    private int totalPages;
}
