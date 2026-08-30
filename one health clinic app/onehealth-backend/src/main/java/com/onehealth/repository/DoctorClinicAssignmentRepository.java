package com.onehealth.repository;

import com.onehealth.entity.DoctorClinicAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorClinicAssignmentRepository extends JpaRepository<DoctorClinicAssignment, Long> {
    List<DoctorClinicAssignment> findByDoctorIdAndActiveTrue(Long doctorId);
    List<DoctorClinicAssignment> findByClinicIdAndActiveTrue(Long clinicId);
    Optional<DoctorClinicAssignment> findByDoctorIdAndClinicId(Long doctorId, Long clinicId);
    boolean existsByDoctorIdAndClinicIdAndActiveTrue(Long doctorId, Long clinicId);
}
