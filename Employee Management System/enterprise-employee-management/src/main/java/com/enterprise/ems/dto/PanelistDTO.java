package com.enterprise.ems.dto;

import com.enterprise.ems.constant.InterviewRecommendation;
import lombok.*;

import java.time.LocalDateTime;

/*
 * One interviewer + their feedback. rating / recommendation / comments are
 * null when the viewer is not yet allowed to see them (blind feedback);
 * "submitted" is always visible so everyone can see who is still pending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelistDTO {
    private Long id;
    private Long interviewerId;
    private String interviewerName;
    private boolean submitted;
    private Integer rating;
    private InterviewRecommendation recommendation;
    private String comments;
    private LocalDateTime submittedAt;
}
