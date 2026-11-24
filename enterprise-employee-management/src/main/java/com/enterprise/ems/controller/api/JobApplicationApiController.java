package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.ApplicationDTO;
import com.enterprise.ems.dto.ApplicationHistoryDTO;
import com.enterprise.ems.dto.HiringRequests.ApplyRequest;
import com.enterprise.ems.dto.HiringRequests.MoveRequest;
import com.enterprise.ems.service.JobApplicationService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* PURPOSE: REST API for the hiring pipeline (a candidate's application to a requisition). */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationApiController {

    private final JobApplicationService applicationService;
    private final HiringAccessService access;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationDTO>> apply(@Valid @RequestBody ApplyRequest body,
                                                             @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Candidate added to requisition",
                applicationService.apply(body.candidateId(), body.requisitionId(), access.actor(principal))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationDTO>> get(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getById(id, access.actor(principal))));
    }

    @GetMapping("/by-requisition/{requisitionId}")
    public ResponseEntity<ApiResponse<List<ApplicationDTO>>> byRequisition(@PathVariable Long requisitionId,
                                                                           @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listByRequisition(requisitionId, access.actor(principal))));
    }

    @GetMapping("/by-candidate/{candidateId}")
    public ResponseEntity<ApiResponse<List<ApplicationDTO>>> byCandidate(@PathVariable Long candidateId,
                                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listByCandidate(candidateId, access.actor(principal))));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<ApiResponse<ApplicationDTO>> move(@PathVariable Long id,
                                                            @Valid @RequestBody MoveRequest body,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Application moved to " + body.toStage(),
                applicationService.move(id, body.toStage(), body.remarks(), access.actor(principal))));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<ApplicationHistoryDTO>>> history(@PathVariable Long id,
                                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.history(id, access.actor(principal))));
    }
}
