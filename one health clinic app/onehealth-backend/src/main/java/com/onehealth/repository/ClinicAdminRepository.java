package com.onehealth.repository;

import com.onehealth.entity.ClinicAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicAdminRepository extends JpaRepository<ClinicAdmin, Long> {
    Optional<ClinicAdmin> findByUserId(Long userId);
    Optional<ClinicAdmin> findByClinicId(Long clinicId);
    List<ClinicAdmin> findByOrganizationId(Long organizationId);
}
