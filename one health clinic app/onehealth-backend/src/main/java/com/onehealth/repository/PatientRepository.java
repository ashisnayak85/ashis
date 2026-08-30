package com.onehealth.repository;

import com.onehealth.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    List<Patient> findByOrganizationIdAndPhone(Long organizationId, String phone);
    List<Patient> findByOrganizationId(Long organizationId);
}
