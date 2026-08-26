package com.doctorapp.repository;

import com.doctorapp.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    Optional<Rating> findByAppointmentId(Long appointmentId);

    Page<Rating> findByDoctorIdAndStatusOrderByCreatedAtDesc(
            Long doctorId, Rating.RatingStatus status, Pageable pageable);

    // COALESCE so a doctor with zero ratings gets 0.0 instead of null.
    @Query("select coalesce(avg(r.rating), 0) from Rating r " +
           "where r.doctor.id = :doctorId and r.status = 'VISIBLE'")
    Double avgRatingForDoctor(@Param("doctorId") Long doctorId);

    @Query("select count(r) from Rating r where r.doctor.id = :doctorId and r.status = 'VISIBLE'")
    long countVisibleForDoctor(@Param("doctorId") Long doctorId);

    // Raw [star, count] pairs for the 1-5 star distribution bar chart. Only
    // stars that actually have at least one rating come back - the service
    // layer fills in the missing 0-count stars so the UI always has all 5 rows.
    @Query("select r.rating as star, count(r) as cnt from Rating r " +
           "where r.doctor.id = :doctorId and r.status = 'VISIBLE' group by r.rating")
    List<Object[]> ratingDistributionRaw(@Param("doctorId") Long doctorId);
}
