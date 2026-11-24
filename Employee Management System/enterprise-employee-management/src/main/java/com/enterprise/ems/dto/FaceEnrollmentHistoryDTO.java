package com.enterprise.ems.dto;

import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: One row of "what the enrollment used to be before it was
 * replaced" - shown to admins in the Face Management history view. Never
 * exposes the raw embedding numbers to the frontend (no reason to; they're
 * meaningless outside the matching model), just the audit-relevant facts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceEnrollmentHistoryDTO {
    private Long id;
    private LocalDateTime originallyCapturedAt;
    private LocalDateTime replacedAt;
    private String replacedBy;
    private boolean hasPhoto;
}
