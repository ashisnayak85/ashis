package com.onehealth.repository;

import com.onehealth.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByAppointmentId(Long appointmentId);

    /** Unbilled prescriptions waiting at this branch's counter - the clinic admin's billing queue. */
    List<Prescription> findByClinicIdAndStatusOrderByCreatedAtAsc(Long clinicId, Prescription.PrescriptionStatus status);
}
