package com.doctorapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String qualification;

    private Integer experienceYears;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;
    
    private java.time.LocalDate dob;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    private String profileImageUrl;

    // A doctor is not publicly searchable/bookable until an admin verifies them.
    @Builder.Default
    private boolean verified = false;

    @Builder.Default
    private boolean active = true;

    // Denormalized cache of ratings.rating aggregated for this doctor, kept in
    // sync by RatingService on every rating write (see RatingService.recalculateDoctorRating).
    // Stored so doctor listing/search pages can show a star rating without an
    // extra aggregate query per doctor. avgRating is rounded to 1 decimal place,
    // e.g. 4.6 - never rounded to a whole star (see product notes on why).
    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "doctor_specializations",
        joinColumns = @JoinColumn(name = "doctor_id"),
        inverseJoinColumns = @JoinColumn(name = "specialization_id")
    )
    @Builder.Default
    private Set<Specialization> specializations = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
