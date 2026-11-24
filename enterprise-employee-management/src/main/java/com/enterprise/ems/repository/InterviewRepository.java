package com.enterprise.ems.repository;

import com.enterprise.ems.constant.InterviewStatus;
import com.enterprise.ems.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationIdOrderByRoundNumberAsc(Long applicationId);

    @Query("SELECT MAX(i.roundNumber) FROM Interview i WHERE i.application.id = :applicationId")
    Integer findMaxRoundNumber(@Param("applicationId") Long applicationId);

    boolean existsByApplicationIdAndStatus(Long applicationId, InterviewStatus status);

    // Interviews this employee sits on, soonest first.
    @Query("SELECT DISTINCT i FROM Interview i JOIN i.panel p WHERE p.interviewer.id = :empId ORDER BY i.scheduledAt DESC")
    List<Interview> findForInterviewer(@Param("empId") Long employeeId);

    // Access check: is this employee on ANY interview panel for this candidate?
    boolean existsByApplication_Candidate_IdAndPanel_Interviewer_Id(Long candidateId, Long interviewerId);

    // Access check: is this employee on ANY interview panel for this application?
    boolean existsByApplication_IdAndPanel_Interviewer_Id(Long applicationId, Long interviewerId);

    long countByStatusAndScheduledAtBetween(InterviewStatus status, LocalDateTime from, LocalDateTime to);
}
