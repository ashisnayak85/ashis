package com.enterprise.ems.repository;

import com.enterprise.ems.entity.FaceEnrollmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaceEnrollmentHistoryRepository extends JpaRepository<FaceEnrollmentHistory, Long> {

    List<FaceEnrollmentHistory> findByEmployeeIdOrderByReplacedAtDesc(Long employeeId);
}
