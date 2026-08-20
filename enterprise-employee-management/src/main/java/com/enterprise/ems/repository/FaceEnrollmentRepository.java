package com.enterprise.ems.repository;

import com.enterprise.ems.entity.FaceEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaceEnrollmentRepository extends JpaRepository<FaceEnrollment, Long> {

    Optional<FaceEnrollment> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);
}
