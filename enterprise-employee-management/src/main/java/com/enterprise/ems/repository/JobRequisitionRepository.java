package com.enterprise.ems.repository;

import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.entity.JobRequisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Long> {

    // Hiring staff (admin / recruiter): every requisition. An empty keyword matches all.
    @Query("SELECT r FROM JobRequisition r WHERE (:status IS NULL OR r.status = :status) AND " +
           "(LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(r.requisitionCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<JobRequisition> searchAll(@Param("status") RequisitionStatus status,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    // Everyone else: only requisitions they raised, manage, recruit for, or are an approver on.
    @Query("SELECT DISTINCT r FROM JobRequisition r LEFT JOIN r.approvals a WHERE " +
           "(r.requestedBy.id = :empId OR r.hiringManager.id = :empId OR r.recruiter.id = :empId OR a.approverEmployee.id = :empId) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(r.requisitionCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<JobRequisition> searchVisibleTo(@Param("empId") Long employeeId,
                                         @Param("status") RequisitionStatus status,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    List<JobRequisition> findByStatusOrderByCreatedAtDesc(RequisitionStatus status);

    long countByStatus(RequisitionStatus status);

    // Total still-unfilled positions across requisitions in the given statuses.
    @Query("SELECT COALESCE(SUM(r.openings - r.filledCount), 0) FROM JobRequisition r WHERE r.status IN :statuses")
    long sumUnfilledPositions(@Param("statuses") Collection<RequisitionStatus> statuses);

    // Requisitions waiting on this employee's approval AT THE CURRENT STAGE
    // (the lowest still-PENDING sequence) - stage N+1 only shows up once N is done.
    @Query("SELECT DISTINCT a.requisition FROM RequisitionApproval a WHERE a.approverEmployee.id = :empId " +
           "AND a.status = com.enterprise.ems.constant.ApprovalStatus.PENDING " +
           "AND a.requisition.status = com.enterprise.ems.constant.RequisitionStatus.PENDING_APPROVAL " +
           "AND a.sequenceOrder = (SELECT MIN(b.sequenceOrder) FROM RequisitionApproval b " +
           "WHERE b.requisition = a.requisition AND b.status = com.enterprise.ems.constant.ApprovalStatus.PENDING)")
    List<JobRequisition> findPendingApprovalFor(@Param("empId") Long employeeId);

    @Query("SELECT COUNT(DISTINCT a.requisition) FROM RequisitionApproval a WHERE a.status = com.enterprise.ems.constant.ApprovalStatus.PENDING " +
           "AND a.requisition.status = com.enterprise.ems.constant.RequisitionStatus.PENDING_APPROVAL")
    long countAwaitingApproval();
}
