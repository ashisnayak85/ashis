package com.doctorapp.repository;

import com.doctorapp.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    List<DoctorAvailability> findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(
            Long doctorId, Long clinicId, DayOfWeek dayOfWeek);

    List<DoctorAvailability> findByDoctorIdAndActiveTrue(Long doctorId);
}
