package com.onehealth.repository;

import com.onehealth.entity.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    List<SalaryRecord> findByEmployeeProfileIdOrderByEffectiveFromDesc(Long employeeProfileId);
}
