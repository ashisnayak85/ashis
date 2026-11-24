package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.constant.OfferStatus;
import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.dto.ApplicationDTO;
import com.enterprise.ems.dto.ApplicationHistoryDTO;
import com.enterprise.ems.entity.Candidate;
import com.enterprise.ems.entity.JobApplication;
import com.enterprise.ems.entity.JobRequisition;
import com.enterprise.ems.entity.Offer;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.HiringMapper;
import com.enterprise.ems.repository.*;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.JobApplicationService;
import com.enterprise.ems.service.hiring.ApplicationStageHelper;
import com.enterprise.ems.service.hiring.HiringAccessService;
import com.enterprise.ems.service.hiring.HiringActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
 * PURPOSE: The hiring pipeline - applying a candidate to a requisition and moving
 * the application through its stages. All stage changes go through ApplicationStageHelper
 * so the transition rules and the history log are always applied.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository historyRepository;
    private final CandidateRepository candidateRepository;
    private final JobRequisitionRepository requisitionRepository;
    private final OfferRepository offerRepository;
    private final ApplicationStageHelper stageHelper;
    private final HiringAccessService access;
    private final HiringMapper mapper;
    private final AuditService auditService;

    @Override
    public ApplicationDTO apply(Long candidateId, Long requisitionId, HiringActor actor) {
        access.requireStaff(actor);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        if (!Boolean.TRUE.equals(candidate.getActive())) {
            throw new BusinessException("This candidate's data has been anonymised and cannot be added to a requisition");
        }
        JobRequisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + requisitionId));
        if (requisition.getStatus() != RequisitionStatus.OPEN) {
            throw new BusinessException("Candidates can only be added to an OPEN requisition (current status: " + requisition.getStatus() + ")");
        }
        if (applicationRepository.existsByCandidateIdAndRequisitionId(candidateId, requisitionId)) {
            throw new DuplicateResourceException("This candidate has already been added to this requisition");
        }

        JobApplication application = applicationRepository.save(JobApplication.builder()
                .candidate(candidate)
                .requisition(requisition)
                .stage(ApplicationStage.APPLIED)
                .appliedOn(LocalDate.now())
                .stageChangedAt(LocalDateTime.now())
                .createdBy(actor.employee())
                .build());
        stageHelper.recordCreated(application, actor.employee());

        auditService.log("APPLY", "JobApplication", application.getId(),
                candidate.getFullName() + " added to " + requisition.getRequisitionCode());
        return mapper.toDTO(application, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDTO getById(Long id, HiringActor actor) {
        JobApplication application = getOrThrow(id);
        access.requireCanViewApplication(actor, application);
        return mapper.toDTO(application, actor.staff());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDTO> listByRequisition(Long requisitionId, HiringActor actor) {
        JobRequisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + requisitionId));
        access.requireCanWorkPipeline(actor, requisition);
        return applicationRepository.findByRequisitionIdOrderByStageChangedAtDesc(requisitionId).stream()
                .map(a -> mapper.toDTO(a, actor.staff()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDTO> listByCandidate(Long candidateId, HiringActor actor) {
        access.requireStaff(actor);
        return applicationRepository.findByCandidateIdOrderByAppliedOnDesc(candidateId).stream()
                .map(a -> mapper.toDTO(a, true))
                .toList();
    }

    @Override
    public ApplicationDTO move(Long id, ApplicationStage toStage, String remarks, HiringActor actor) {
        JobApplication application = getOrThrow(id);
        JobRequisition requisition = application.getRequisition();
        access.requireCanWorkPipeline(actor, requisition);

        if (toStage == ApplicationStage.OFFER) {
            throw new BusinessException("The OFFER stage is entered by creating an offer for this application");
        }
        if (toStage == ApplicationStage.HIRED) {
            throw new BusinessException("HIRED is set automatically when an accepted offer is converted to an employee");
        }
        boolean closing = toStage == ApplicationStage.REJECTED || toStage == ApplicationStage.WITHDRAWN;
        if (closing && (remarks == null || remarks.isBlank())) {
            throw new BusinessException("A reason is required to reject or withdraw an application");
        }
        // Moving a candidate FORWARD only makes sense while the requisition is being actively worked.
        if (!closing && requisition.getStatus() != RequisitionStatus.OPEN) {
            throw new BusinessException("The requisition is " + requisition.getStatus() + " - candidates can only be moved forward while it is OPEN");
        }

        stageHelper.transition(application, toStage, actor.employee(), remarks == null ? null : remarks.trim());

        if (closing) {
            revokeOpenOffers(application, "Application " + toStage + ": " + remarks.trim());
        }
        auditService.log("MOVE_STAGE", "JobApplication", application.getId(),
                application.getCandidate().getFullName() + " -> " + toStage + " (" + requisition.getRequisitionCode() + ")");
        return mapper.toDTO(application, actor.staff());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationHistoryDTO> history(Long id, HiringActor actor) {
        JobApplication application = getOrThrow(id);
        access.requireCanViewApplication(actor, application);
        return historyRepository.findByApplicationIdOrderByChangedAtAscIdAsc(id).stream()
                .map(mapper::toDTO)
                .toList();
    }

    // ============================================================ helpers

    private JobApplication getOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    // Rejecting/withdrawing a candidate must not leave a live offer behind.
    private void revokeOpenOffers(JobApplication application, String reason) {
        List<Offer> open = offerRepository.findByApplicationIdAndStatusIn(application.getId(), OfferStatus.OPEN_STATUSES);
        for (Offer offer : open) {
            offer.setStatus(OfferStatus.REVOKED);
            offer.setClosingReason(reason.length() > 500 ? reason.substring(0, 500) : reason);
            offerRepository.save(offer);
        }
    }
}
