package com.onehealth.repository;

import com.onehealth.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDateOrderByStartTimeAsc(Long doctorId, LocalDate date);

    List<Appointment> findByClinicIdAndAppointmentDateOrderByStartTimeAsc(Long clinicId, LocalDate date);

    /** Core dashboard query: every appointment across the org's clinics within a date range. */
    List<Appointment> findByOrganizationIdAndAppointmentDateBetween(
            Long organizationId, LocalDate from, LocalDate to);

    /** Same, scoped to one branch - used by a clinic admin's own dashboard view. */
    List<Appointment> findByOrganizationIdAndClinicIdAndAppointmentDateBetween(
            Long organizationId, Long clinicId, LocalDate from, LocalDate to);
}
