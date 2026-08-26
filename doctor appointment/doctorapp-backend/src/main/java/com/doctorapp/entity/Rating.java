package com.doctorapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A patient's rating + optional review text for one completed appointment.
 *
 * Tied 1:1 to the appointment (not just to doctor+patient) so that:
 *   - only a patient who actually had that visit can rate it ("verified visit",
 *     same idea as Flipkart/Amazon's "verified purchase")
 *   - a patient can't submit multiple ratings for the same visit (unique FK)
 *   - a patient CAN rate the same doctor again after a later, separate visit
 *
 * Doctor.avgRating / Doctor.ratingCount are a denormalized cache of what you'd
 * get by aggregating this table, recalculated by RatingService on every write
 * so the two never drift apart. This table stays the source of truth (needed
 * for the star-distribution breakdown, review listing, and any future
 * moderation).
 */
@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /** 1-5 stars. */
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_text", length = 1000)
    private String reviewText;

    // Kept VISIBLE unless a future moderation flow hides/flags it. Aggregates
    // and the public review list only ever consider VISIBLE rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RatingStatus status = RatingStatus.VISIBLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RatingStatus {
        VISIBLE, HIDDEN, FLAGGED
    }
}
