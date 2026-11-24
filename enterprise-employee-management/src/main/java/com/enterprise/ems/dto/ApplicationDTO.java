package com.enterprise.ems.dto;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.constant.CandidateSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/* Response shape for one application (a candidate in a requisition's pipeline). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {
    private Long id;

    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidateMobile;
    private BigDecimal candidateExperience;
    private String candidateCurrentCompany;
    private Integer candidateNoticePeriodDays;
    private BigDecimal candidateExpectedCtc;             // null unless the viewer is hiring staff
    private CandidateSource candidateSource;

    private Long requisitionId;
    private String requisitionCode;
    private String requisitionTitle;

    private ApplicationStage stage;
    private String rejectionReason;
    private LocalDate appliedOn;
    private LocalDateTime stageChangedAt;
    private Long hiredEmployeeId;

    // Stages the "move" action may target from here (OFFER / HIRED are driven by their own flows).
    private List<ApplicationStage> allowedNextStages;
}
