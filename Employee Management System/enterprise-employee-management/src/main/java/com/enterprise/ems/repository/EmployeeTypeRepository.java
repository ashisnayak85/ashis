package com.enterprise.ems.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.enterprise.ems.entity.EmployeeType;
import java.util.Optional;

@Repository
public interface EmployeeTypeRepository extends JpaRepository<EmployeeType, Long> {
	 Optional<EmployeeType> findByCode(String code);

	    boolean existsByName(String name);

	    boolean existsByCode(String code);

	    // Resignation no-dues clearance: every active department gets a
	    // clearance row when a resignation enters CLEARANCE_PENDING - see
	    // ResignationServiceImpl.enterClearance().
	    java.util.List<EmployeeType> findByActiveTrue();
}
