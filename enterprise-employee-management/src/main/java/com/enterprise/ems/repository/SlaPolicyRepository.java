package com.enterprise.ems.repository;

import com.enterprise.ems.entity.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByDepartmentIdAndPriority(Long departmentId, String priority);

    List<SlaPolicy> findByDepartmentId(Long departmentId);
}
