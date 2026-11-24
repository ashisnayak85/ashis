package com.enterprise.ems.service;

import com.enterprise.ems.constant.ApplicationStage;
import com.enterprise.ems.dto.ApplicationDTO;
import com.enterprise.ems.dto.ApplicationHistoryDTO;
import com.enterprise.ems.service.hiring.HiringActor;

import java.util.List;

public interface JobApplicationService {

    // Applies a candidate to an OPEN requisition (stage APPLIED).
    ApplicationDTO apply(Long candidateId, Long requisitionId, HiringActor actor);

    ApplicationDTO getById(Long id, HiringActor actor);

    List<ApplicationDTO> listByRequisition(Long requisitionId, HiringActor actor);

    List<ApplicationDTO> listByCandidate(Long candidateId, HiringActor actor);

    // Moves through the pipeline. OFFER and HIRED are not valid targets here - they are driven by the offer flow.
    ApplicationDTO move(Long id, ApplicationStage toStage, String remarks, HiringActor actor);

    List<ApplicationHistoryDTO> history(Long id, HiringActor actor);
}
