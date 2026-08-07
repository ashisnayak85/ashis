package com.enterprise.ems.repository;

import com.enterprise.ems.entity.LeaveMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveMaster, Long> {

    Page<LeaveMaster> findByEmployeeId(Long employeeId, Pageable pageable);

    List<LeaveMaster> findByStatus(String status);

    long countByStatus(String status);
}
