package com.onehealth.repository;

import com.onehealth.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    List<StockBatch> findByClinicIdAndMedicineIdOrderByExpiryDateAsc(Long clinicId, Long medicineId);

    /** FEFO fetch: earliest-expiring batches with stock left, first. */
    List<StockBatch> findByClinicIdAndMedicineIdAndQuantityRemainingGreaterThanOrderByExpiryDateAsc(
            Long clinicId, Long medicineId, Integer minQuantity);

    List<StockBatch> findByClinicId(Long clinicId);
}
