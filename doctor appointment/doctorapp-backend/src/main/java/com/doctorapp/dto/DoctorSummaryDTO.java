package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Admin-facing view of a doctor: includes email (from User) and verification/active flags. */
@Getter
@Builder
@AllArgsConstructor
public class DoctorSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String qualification;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private boolean verified;
    private boolean active;
    private Double avgRating;
    private Integer ratingCount;
    private List<String> specializations;
    private int clinicCount;
    private LocalDateTime createdAt;
}
