package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    // Resignation no-dues clearance: every active department gets a
    // clearance row when a resignation enters CLEARANCE_PENDING - see
    // ResignationServiceImpl.enterClearance().
    java.util.List<Department> findByActiveTrue();
}
