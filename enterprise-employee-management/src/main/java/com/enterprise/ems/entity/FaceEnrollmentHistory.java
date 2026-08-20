package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: Snapshot of a FaceEnrollment taken right before it gets
 * overwritten by a re-enrollment. Nothing is ever destructively lost when
 * someone re-enrolls (self or admin-triggered) - the previous embedding and
 * reference photo path are preserved here, alongside who replaced it and
 * when, so an admin can audit or manually roll back a bad re-enrollment.
 *
 * Only written on an UPDATE (an employee who already had an enrollment).
 * A brand new first-time enrollment has nothing to back up.
 */
@Entity
@Table(name = "face_enrollment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceEnrollmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    // Same LONGTEXT reasoning as FaceEnrollment.embedding.
    @Lob
    @Column(name = "embedding", nullable = false, columnDefinition = "LONGTEXT")
    private String embedding;

    @Column(name = "reference_photo_path")
    private String referencePhotoPath;

    // When the enrollment being backed up was ORIGINALLY captured (copied
    // from the FaceEnrollment row's own createdAt/updatedAt at backup time).
    @Column(name = "originally_captured_at")
    private LocalDateTime originallyCapturedAt;

    // Who triggered the re-enrollment that caused this backup - a username,
    // or "self" when the employee re-enrolled their own face.
    @Column(name = "replaced_by", length = 100)
    private String replacedBy;

    @CreationTimestamp
    @Column(name = "replaced_at", updatable = false)
    private LocalDateTime replacedAt;
}
