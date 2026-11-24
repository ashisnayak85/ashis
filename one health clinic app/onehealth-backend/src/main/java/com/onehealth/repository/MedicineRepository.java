package com.onehealth.repository;

import com.onehealth.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByOrganizationId(Long organizationId);
    List<Medicine> findByOrganizationIdAndActiveTrue(Long organizationId);
}
