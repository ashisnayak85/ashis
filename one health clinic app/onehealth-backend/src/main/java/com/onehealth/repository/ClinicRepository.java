package com.onehealth.repository;

import com.onehealth.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    List<Clinic> findByOrganizationId(Long organizationId);
    List<Clinic> findByOrganizationIdAndActiveTrue(Long organizationId);
}
