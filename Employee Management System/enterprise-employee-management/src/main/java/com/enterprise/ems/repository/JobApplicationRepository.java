package com.enterprise.ems.repository;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByRequisitionIdOrderByStageChangedAtDesc(Long requisitionId);

    List<JobApplication> findByCandidateIdOrderByAppliedOnDesc(Long candidateId);

    boolean existsByCandidateIdAndRequisitionId(Long candidateId, Long requisitionId);

    long countByRequisitionId(Long requisitionId);

    // Hiring-manager access check for a candidate (they only see candidates who applied to THEIR requisitions).
    boolean existsByCandidateIdAndRequisition_HiringManager_Id(Long candidateId, Long hiringManagerId);

    // Pipeline counts for requisitions that are currently being worked on.
    @Query("SELECT a.stage, COUNT(a) FROM JobApplication a WHERE a.requisition.status IN :statuses GROUP BY a.stage")
    List<Object[]> countByStageForRequisitionStatuses(@Param("statuses") Collection<com.enterprise.ems.constant.RequisitionStatus> statuses);

    List<JobApplication> findTop500ByStageOrderByStageChangedAtDesc(ApplicationStage stage);
}
