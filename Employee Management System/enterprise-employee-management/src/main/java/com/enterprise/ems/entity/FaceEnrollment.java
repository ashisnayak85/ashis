package com.enterprise.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * PURPOSE: One row per employee - their "reference face" for self-attendance
 * verification (see FaceRecognitionService). Captured once at enrollment,
 * re-enrollable later (e.g. after a haircut, glasses, or a bad first capture).
 *
 * WHY NOT REUSE Employee.profilePhoto: that field is a display picture HR or
 * the employee uploads for the directory - often not a clean frontal shot,
 * sometimes outdated. This enrollment is captured specifically for matching,
 * ideally under supervision (HR desk / first login), so it's a separate,
 * purpose-built record.
 *
 * WHAT'S STORED: not the raw photo as the thing we compare - a "face
 * embedding" (a list of numbers a face-recognition model derives from the
 * photo, see FaceRecognitionServiceImpl). Storing numbers rather than
 * repeatedly re-analyzing an image is faster and is the normal approach.
 * referencePhotoPath optionally keeps the original captured image on disk
 * (via FileStorageService) purely so an admin can visually audit/re-enroll -
 * it is never itself used for the comparison.
 */
@Entity
@Table(name = "face_enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    // Comma-separated float vector (the face embedding). Stored as text rather
    // than a binary/vector column so this works unmodified on plain MySQL -
    // no vector extension required, and the vectors here are small (a few
    // hundred numbers), so this is not a performance concern at EMS scale.
    //
    // columnDefinition is pinned explicitly to LONGTEXT rather than relying on
    // @Lob's default DDL mapping: a ~512-number CSV string runs several
    // thousand characters, and depending on Hibernate/dialect version, a bare
    // @Lob on a String can generate a column far smaller than that (this is
    // exactly what caused the "Data too long for column 'embedding'" error).
    // LONGTEXT (up to 4GB) removes all ambiguity.
    @Lob
    @Column(name = "embedding", nullable = false, columnDefinition = "LONGTEXT")
    private String embedding;

    // Optional: where the original enrollment photo is stored on disk, for
    // admin review only - never used in the actual comparison.
    @Column(name = "reference_photo_path")
    private String referencePhotoPath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
