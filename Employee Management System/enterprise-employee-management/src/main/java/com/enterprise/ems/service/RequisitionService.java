package com.enterprise.ems.service;

import com.enterprise.ems.dto.HiringRequests.RequisitionAction;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.RequisitionDTO;
import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.service.hiring.HiringActor;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RequisitionService {

    RequisitionDTO create(RequisitionDTO dto, HiringActor actor);

    // Only DRAFT or REJECTED requisitions can be edited; editing a REJECTED one reopens it as DRAFT.
    RequisitionDTO update(Long id, RequisitionDTO dto, HiringActor actor);

    // Resolves the REQUISITION approval chain and starts approval (or auto-approves if no chain is configured).
    RequisitionDTO submit(Long id, HiringActor actor);

    RequisitionDTO approve(Long id, String remarks, HiringActor actor);

    RequisitionDTO reject(Long id, String remarks, HiringActor actor);

    // OPEN / HOLD / RESUME / CLOSE / CANCEL
    RequisitionDTO changeStatus(Long id, RequisitionAction action, String remarks, HiringActor actor);

    RequisitionDTO assignRecruiter(Long id, Long recruiterEmployeeId, HiringActor actor);

    RequisitionDTO getById(Long id, HiringActor actor);

    PageResponse<RequisitionDTO> search(String keyword, RequisitionStatus status, Pageable pageable, HiringActor actor);

    List<RequisitionDTO> pendingApprovals(HiringActor actor);
}
