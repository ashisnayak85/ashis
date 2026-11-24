package com.onehealth.repository;

import com.onehealth.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByOrganizationId(Long organizationId);
    List<Doctor> findByOrganizationIdAndActiveTrue(Long organizationId);
}
