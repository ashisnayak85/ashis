package com.enterprise.ems.repository;

import com.enterprise.ems.constant.OfferStatus;
import com.enterprise.ems.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    List<Offer> findByApplicationIdAndStatusIn(Long applicationId, Collection<OfferStatus> statuses);

    boolean existsByApplicationIdAndStatusIn(Long applicationId, Collection<OfferStatus> statuses);

    @Query("SELECT o FROM Offer o WHERE (:status IS NULL OR o.status = :status) AND " +
           "(LOWER(o.application.candidate.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(o.application.candidate.lastName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.application.requisition.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Offer> search(@Param("status") OfferStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT a.offer FROM OfferApproval a WHERE a.approverEmployee.id = :empId " +
           "AND a.status = com.enterprise.ems.constant.ApprovalStatus.PENDING " +
           "AND a.offer.status = com.enterprise.ems.constant.OfferStatus.PENDING_APPROVAL " +
           "AND a.sequenceOrder = (SELECT MIN(b.sequenceOrder) FROM OfferApproval b " +
           "WHERE b.offer = a.offer AND b.status = com.enterprise.ems.constant.ApprovalStatus.PENDING)")
    List<Offer> findPendingApprovalFor(@Param("empId") Long employeeId);

    @Query("SELECT o.status, COUNT(o) FROM Offer o GROUP BY o.status")
    List<Object[]> countByStatus();

    // Offers that were sent but whose expiry date has passed - picked up by the nightly expiry job.
    List<Offer> findByStatusAndExpiryDateBefore(OfferStatus status, java.time.LocalDate date);
}
