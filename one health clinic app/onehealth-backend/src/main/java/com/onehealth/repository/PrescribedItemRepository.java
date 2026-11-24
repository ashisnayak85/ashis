package com.onehealth.repository;

import com.onehealth.entity.PrescribedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescribedItemRepository extends JpaRepository<PrescribedItem, Long> {
    List<PrescribedItem> findByPrescriptionId(Long prescriptionId);
}
