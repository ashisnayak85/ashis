package com.onehealth.repository;

import com.onehealth.entity.AppointmentSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByDoctorIdAndClinicIdAndDateOrderByStartTimeAsc(
            Long doctorId, Long clinicId, LocalDate date);

    /**
     * Every slot already generated (materialized) for this doctor+clinic on or
     * after a given date, regardless of day-of-week - used to re-sync existing
     * future slots when the doctor's weekly template changes. Only dates that
     * already have rows here need fixing; a future date nobody has viewed yet
     * will simply generate correctly from the current template whenever it's
     * first requested, so there's no need to pre-generate anything.
     */
    List<AppointmentSlot> findByDoctorIdAndClinicIdAndDateGreaterThanEqual(
            Long doctorId, Long clinicId, LocalDate from);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);

    long countByClinicIdInAndDateBetween(List<Long> clinicIds, LocalDate from, LocalDate to);
}
