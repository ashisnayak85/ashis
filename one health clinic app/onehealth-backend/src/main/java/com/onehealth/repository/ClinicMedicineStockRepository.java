package com.onehealth.repository;

import com.onehealth.entity.ClinicMedicineStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicMedicineStockRepository extends JpaRepository<ClinicMedicineStock, Long> {
    List<ClinicMedicineStock> findByClinicId(Long clinicId);
    Optional<ClinicMedicineStock> findByClinicIdAndMedicineId(Long clinicId, Long medicineId);
}
