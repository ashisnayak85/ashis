package com.onehealth.repository;

import com.onehealth.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    List<Specialization> findByOrganizationId(Long organizationId);
    List<Specialization> findByOrganizationIdAndActiveTrue(Long organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
}
