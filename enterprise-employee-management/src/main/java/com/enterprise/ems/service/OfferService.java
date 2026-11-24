package com.enterprise.ems.service;

import com.enterprise.ems.constant.OfferStatus;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.HiringRequests.ConvertRequest;
import com.enterprise.ems.dto.OfferDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.service.hiring.HiringActor;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OfferService {

    OfferDTO create(Long applicationId, OfferDTO dto, HiringActor actor);

    // DRAFT only.
    OfferDTO update(Long id, OfferDTO dto, HiringActor actor);

    OfferDTO submit(Long id, HiringActor actor);

    OfferDTO approve(Long id, String remarks, HiringActor actor);

    OfferDTO reject(Long id, String remarks, HiringActor actor);

    // APPROVED -> SENT (notifies the candidate).
    OfferDTO send(Long id, HiringActor actor);

    // Candidate's answer, recorded by a recruiter.
    OfferDTO accept(Long id, HiringActor actor);

    OfferDTO decline(Long id, String reason, HiringActor actor);

    OfferDTO revoke(Long id, String reason, HiringActor actor);

    // ACCEPTED offer -> new Employee (through EmployeeService.create) -> application HIRED.
    EmployeeDTO convertToEmployee(Long id, ConvertRequest request, HiringActor actor);

    OfferDTO getById(Long id, HiringActor actor);

    List<OfferDTO> listByApplication(Long applicationId, HiringActor actor);

    PageResponse<OfferDTO> search(String keyword, OfferStatus status, Pageable pageable, HiringActor actor);

    List<OfferDTO> pendingApprovals(HiringActor actor);

    // Printable offer letter as HTML (the browser can print/save it as PDF).
    String renderLetterHtml(Long id, HiringActor actor);
}
