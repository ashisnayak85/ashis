package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Resignation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ResignationRepository extends JpaRepository<Resignation, Long>, JpaSpecificationExecutor<Resignation> {

    // A resigning employee should only ever have one non-terminal resignation
    // in flight - guard this in the service before insert, and use this to
    // check it.
    List<Resignation> findByEmployeeIdAndStatusNotIn(Long employeeId, List<String> terminalStatuses);

    Page<Resignation> findByStatus(String status, Pageable pageable);

    Optional<Resignation> findTopByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
