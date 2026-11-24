package com.enterprise.ems.controller.api;

import com.enterprise.ems.constant.RequisitionStatus;
import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.HiringRequests.AssignRecruiterRequest;
import com.enterprise.ems.dto.HiringRequests.RemarksRequest;
import com.enterprise.ems.dto.HiringRequests.StatusActionRequest;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.RequisitionDTO;
import com.enterprise.ems.service.RequisitionService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import com.enterprise.ems.service.hiring.HiringActor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: REST API for job requisitions. Authentication is enforced by SecurityConfig
 * (anyRequest().authenticated()); WHO may do WHAT is decided in the service layer via
 * HiringAccessService, because it depends on the record (owner / approver / recruiter),
 * not just on a role - so there is deliberately no blanket @PreAuthorize here.
 */
@RestController
@RequestMapping("/api/requisitions")
@RequiredArgsConstructor
public class RequisitionApiController {

    private final RequisitionService requisitionService;
    private final HiringAccessService access;

    @PostMapping
    public ResponseEntity<ApiResponse<RequisitionDTO>> create(@Valid @RequestBody RequisitionDTO dto,
                                                              @AuthenticationPrincipal UserDetails principal) {
        HiringActor actor = access.actor(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Requisition created", requisitionService.create(dto, actor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RequisitionDTO>> update(@PathVariable Long id,
                                                              @Valid @RequestBody RequisitionDTO dto,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Requisition updated",
                requisitionService.update(id, dto, access.actor(principal))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<RequisitionDTO>> submit(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Requisition submitted",
                requisitionService.submit(id, access.actor(principal))));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<RequisitionDTO>> approve(@PathVariable Long id,
                                                               @Valid @RequestBody(required = false) RemarksRequest body,
                                                               @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Requisition approved",
                requisitionService.approve(id, body == null ? null : body.remarks(), access.actor(principal))));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RequisitionDTO>> reject(@PathVariable Long id,
                                                              @Valid @RequestBody RemarksRequest body,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Requisition rejected",
                requisitionService.reject(id, body.remarks(), access.actor(principal))));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RequisitionDTO>> changeStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody StatusActionRequest body,
                                                                    @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Requisition updated",
                requisitionService.changeStatus(id, body.action(), body.remarks(), access.actor(principal))));
    }

    @PostMapping("/{id}/assign-recruiter")
    public ResponseEntity<ApiResponse<RequisitionDTO>> assignRecruiter(@PathVariable Long id,
                                                                        @Valid @RequestBody AssignRecruiterRequest body,
                                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Recruiter assigned",
                requisitionService.assignRecruiter(id, body.recruiterId(), access.actor(principal))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<RequisitionDTO>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                requisitionService.search(keyword, status, pageable, access.actor(principal))));
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<RequisitionDTO>>> pendingApprovals(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(requisitionService.pendingApprovals(access.actor(principal))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequisitionDTO>> get(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(requisitionService.getById(id, access.actor(principal))));
    }
}
