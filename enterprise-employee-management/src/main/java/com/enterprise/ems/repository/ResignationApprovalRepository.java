package com.enterprise.ems.repository;

import com.enterprise.ems.entity.ResignationApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResignationApprovalRepository extends JpaRepository<ResignationApproval, Long> {

    // "My pending approvals" inbox - PENDING rows assigned to this approver
    List<ResignationApproval> findByApproverEmployeeIdAndStatus(Long approverEmployeeId, String status);

    // The next actionable stage for a resignation - lowest sequenceOrder
    // still PENDING. Enforces "stage N+1 can't be actioned before stage N".
    Optional<ResignationApproval> findFirstByResignationIdAndStatusOrderBySequenceOrderAsc(Long resignationId, String status);

    List<ResignationApproval> findByResignationIdOrderBySequenceOrderAsc(Long resignationId);
}
