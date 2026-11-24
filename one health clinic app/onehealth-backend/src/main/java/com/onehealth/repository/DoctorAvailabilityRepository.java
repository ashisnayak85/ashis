package com.onehealth.repository;

import com.onehealth.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(
            Long doctorId, Long clinicId, DayOfWeek dayOfWeek);

    List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    List<DoctorAvailability> findByClinicIdOrderByDayOfWeekAscStartTimeAsc(Long clinicId);

    /**
     * This doctor's other ACTIVE templates (at ANY branch of the org) on the same
     * day whose time range overlaps [startTime, endTime). This is the rule that
     * stops a doctor being scheduled at two branches (or twice at the same branch)
     * at the same time - see requirement: "if a doctor slot booked for a particular
     * time period, same doctor cannot be booked/available for other location".
     */
    @Query("select a from DoctorAvailability a " +
           "where a.doctor.id = :doctorId and a.dayOfWeek = :dayOfWeek and a.active = true " +
           "and a.startTime < :endTime and a.endTime > :startTime")
    List<DoctorAvailability> findOverlapping(@Param("doctorId") Long doctorId,
                                              @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                              @Param("startTime") LocalTime startTime,
                                              @Param("endTime") LocalTime endTime);
}
