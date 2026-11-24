package com.enterprise.ems.service;

import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.ResignationAssetDTO;
import com.enterprise.ems.dto.ResignationDTO;
import org.springframework.data.domain.Pageable;

public interface ResignationService {

    // --- Employee (self-service) ---

    // noticePeriodDaysOverride is nullable - falls back to
    // AppConstants.DEFAULT_NOTICE_PERIOD_DAYS. Throws if the employee already
    // has a non-terminal resignation in flight.
    ResignationDTO submit(Long employeeId, String reason, Integer noticePeriodDaysOverride);

    ResignationDTO getMyLatest(Long employeeId);

    // Only allowed before FINAL_APPROVED/REJECTED/WITHDRAWN.
    ResignationDTO withdraw(Long resignationId, Long employeeId);

    // --- Viewing ---

    ResignationDTO getById(Long resignationId, Long requesterEmployeeId, boolean privileged);

    PageResponse<ResignationDTO> search(String status, Pageable pageable);

    // --- Approval stages (role-based, sequential) ---

    // "My pending approvals" inbox
    java.util.List<ResignationDTO> getPendingApprovalsForMe(Long approverEmployeeId);

    ResignationDTO approveStage(Long resignationId, Long approverEmployeeId, String remarks);

    ResignationDTO rejectStage(Long resignationId, Long approverEmployeeId, String remarks);

    // --- No-dues clearance (one action per clearing department) ---

    ResignationDTO clearDepartment(Long resignationId, Long departmentId, Long actorEmployeeId, String remarks);

    ResignationDTO blockDepartment(Long resignationId, Long departmentId, Long actorEmployeeId, String remarks);

    // --- Asset return line items, surfaced during clearance ---

    ResignationAssetDTO addAsset(Long resignationId, String assetDescription);

    ResignationDTO markAssetReturned(Long resignationId, Long assetId, String remarks);

    // --- Final approval ---

    // Fails loudly (BusinessException) if clearance isn't CLEARANCE_DONE yet,
    // or if the employee still has open/in-progress ticket work assigned to
    // them - see ResignationServiceImpl.finalApprove() for the full check
    // and how to resolve it (transfer/resolve the ticket first, then retry).
    ResignationDTO finalApprove(Long resignationId, Long approverEmployeeId);

    // Optional override of the last-working-day at any point before
    // FINAL_APPROVED (buyout, early relieving, extension).
    ResignationDTO overrideLastWorkingDay(Long resignationId, java.time.LocalDate actualLastWorkingDay, String overrideReason);
}
