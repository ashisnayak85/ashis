package com.doctorapp.repository;

import com.doctorapp.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    List<DoctorAvailability> findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(
            Long doctorId, Long clinicId, DayOfWeek dayOfWeek);

    List<DoctorAvailability> findByDoctorIdAndActiveTrue(Long doctorId);

    /** Every availability template for this doctor, across all clinics - used to show the doctor their full weekly picture. */
    List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    /** Every availability template at a given clinic - used by the clinic admin to review/manage doctors' hours. */
    List<DoctorAvailability> findByClinicIdOrderByDayOfWeekAscStartTimeAsc(Long clinicId);

    /**
     * Finds this doctor's other ACTIVE availability templates (at any clinic) on the
     * same day whose time range overlaps [startTime, endTime). Two ranges overlap when
     * one starts before the other ends, on both sides. Used to stop a doctor from being
     * scheduled at two clinics (or twice at the same clinic) at the same time.
     */
    @Query("select a from DoctorAvailability a " +
           "where a.doctor.id = :doctorId and a.dayOfWeek = :dayOfWeek and a.active = true " +
           "and a.startTime < :endTime and a.endTime > :startTime")
    List<DoctorAvailability> findOverlapping(@Param("doctorId") Long doctorId,
                                              @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                              @Param("startTime") LocalTime startTime,
                                              @Param("endTime") LocalTime endTime);
}
