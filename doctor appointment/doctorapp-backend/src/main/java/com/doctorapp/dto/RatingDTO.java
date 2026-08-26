package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RatingDTO {
    private Long id;
    private Long appointmentId;
    private String patientName;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
}
