package com.enterprise.ems.dto;

import com.enterprise.ems.constant.InterviewMode;
import com.enterprise.ems.constant.InterviewStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/* Request AND response shape for an interview round. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDTO {

    private Long id;

    private Long applicationId;                          // response only (taken from the URL on create)
    private Long candidateId;                            // response only
    private String candidateName;                        // response only
    private Long requisitionId;                          // response only
    private String requisitionTitle;                     // response only

    private Integer roundNumber;                         // response only (assigned automatically)

    @NotBlank(message = "Round name is required (e.g. Technical, HR)")
    @Size(max = 100, message = "Round name must not exceed 100 characters")
    private String roundName;

    @NotNull(message = "Interview mode is required")
    private InterviewMode mode;

    @NotNull(message = "Interview date and time is required")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer durationMinutes;

    @Size(max = 300, message = "Location / link must not exceed 300 characters")
    private String locationOrLink;

    private InterviewStatus status;                      // response only
    private String cancelReason;                         // response only

    // Request: who sits on the panel.
    private List<Long> interviewerIds;

    // Response: the panel with (visibility-filtered) feedback.
    private List<PanelistDTO> panel;

    private boolean viewerIsPanelist;                    // response only
    private boolean viewerHasSubmitted;                  // response only
}
