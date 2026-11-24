package com.enterprise.ems.repository;

import com.enterprise.ems.entity.ResignationClearance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResignationClearanceRepository extends JpaRepository<ResignationClearance, Long> {

    List<ResignationClearance> findByResignationId(Long resignationId);

    // "My department's pending clearances" inbox
    List<ResignationClearance> findByDepartmentIdAndStatus(Long departmentId, String status);

    long countByResignationIdAndStatusNot(Long resignationId, String status);
}
