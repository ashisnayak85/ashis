package com.enterprise.ems.repository;

import com.enterprise.ems.constant.ApprovalStatus;
import com.enterprise.ems.entity.OfferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApproval, Long> {

    Optional<OfferApproval> findFirstByOfferIdAndStatusOrderBySequenceOrderAsc(Long offerId, ApprovalStatus status);
}
