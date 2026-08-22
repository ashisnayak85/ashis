package com.enterprise.ems.repository;

import com.enterprise.ems.entity.TicketMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMasterRepository extends JpaRepository<TicketMaster, Long>, JpaSpecificationExecutor<TicketMaster> {

    // Scans for the SLA scheduler - see TicketSlaScheduler. Kept as plain
    // derived queries (not Specifications) since these run unconditionally,
    // not against user-supplied optional filters.

    @Query("SELECT t FROM TicketMaster t WHERE t.status = 'OPEN' AND t.acceptanceBreached = false")
    List<TicketMaster> findOpenUnbreachedForAcceptanceCheck();

    @Query("SELECT t FROM TicketMaster t WHERE t.status = 'IN_PROGRESS' AND t.resolutionBreached = false " +
           "AND t.acceptedAt IS NOT NULL")
    List<TicketMaster> findInProgressForResolutionCheck();
}
