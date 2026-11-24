package com.enterprise.ems.repository;

import com.enterprise.ems.constant.ApprovalStatus;
import com.enterprise.ems.entity.RequisitionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequisitionApprovalRepository extends JpaRepository<RequisitionApproval, Long> {

    // The stage that can be actioned RIGHT NOW: lowest sequence still PENDING.
    Optional<RequisitionApproval> findFirstByRequisitionIdAndStatusOrderBySequenceOrderAsc(Long requisitionId, ApprovalStatus status);
}
