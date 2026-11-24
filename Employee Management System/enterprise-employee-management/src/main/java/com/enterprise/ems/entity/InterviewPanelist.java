package com.enterprise.ems.entity;

import com.enterprise.ems.constant.InterviewRecommendation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/*
 * PURPOSE: One interviewer on one interview PLUS that interviewer's
 * feedback (rating, recommendation, comments). Feedback is blind: another
 * panelist can't read it until they have submitted their own (enforced in
 * HiringMapper / InterviewServiceImpl).
 * TABLE: interview_panelist  (unique per interview + interviewer)
 */
@Entity
@Table(name = "interview_panelist",
        uniqueConstraints = @UniqueConstraint(name = "uk_panelist_interview_interviewer",
                columnNames = {"interview_id", "interviewer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPanelist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private Employee interviewer;

    // 1 (poor) .. 5 (excellent); null until feedback is submitted.
    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "recommendation", length = 20)
    private InterviewRecommendation recommendation;

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public boolean isSubmitted() {
        return submittedAt != null;
    }
}
