package com.doctorapp.repository;

import com.doctorapp.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, java.time.LocalDate date);
    List<Appointment> findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(Long doctorId);
    List<Appointment> findAllByOrderByAppointmentDateDescStartTimeDesc();
    long countByAppointmentDate(java.time.LocalDate date);
}
