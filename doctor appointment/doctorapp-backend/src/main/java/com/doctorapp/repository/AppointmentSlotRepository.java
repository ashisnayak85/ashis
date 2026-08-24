package com.doctorapp.repository;

import com.doctorapp.entity.AppointmentSlot;
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

    Optional<AppointmentSlot> findByDoctorIdAndClinicIdAndDateAndStartTime(
            Long doctorId, Long clinicId, LocalDate date, java.time.LocalTime startTime);

    // Pessimistic write lock: the transaction that reads this row wins the right to
    // book it; any concurrent booking attempt on the same slot blocks until this
    // transaction commits/rolls back, then re-checks status. This is what actually
    // prevents double-booking, on top of the DB unique constraint as a second net.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);
}
