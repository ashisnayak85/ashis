package com.enterprise.ems.repository;

import com.enterprise.ems.entity.WorkflowRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRoleRepository extends JpaRepository<WorkflowRole, Long> {

    List<WorkflowRole> findByActiveTrueOrderByNameAsc();

    Optional<WorkflowRole> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
