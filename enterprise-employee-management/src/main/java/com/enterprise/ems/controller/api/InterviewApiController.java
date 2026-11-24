package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.HiringRequests.FeedbackRequest;
import com.enterprise.ems.dto.HiringRequests.InterviewStatusRequest;
import com.enterprise.ems.dto.HiringRequests.ReasonRequest;
import com.enterprise.ems.dto.InterviewDTO;
import com.enterprise.ems.service.InterviewService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* PURPOSE: REST API for interview rounds and interviewer feedback. */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewApiController {

    private final InterviewService interviewService;
    private final HiringAccessService access;

    @PostMapping("/by-application/{applicationId}")
    public ResponseEntity<ApiResponse<InterviewDTO>> schedule(@PathVariable Long applicationId,
                                                              @Valid @RequestBody InterviewDTO dto,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Interview scheduled",
                interviewService.schedule(applicationId, dto, access.actor(principal))));
    }

    @GetMapping("/by-application/{applicationId}")
    public ResponseEntity<ApiResponse<List<InterviewDTO>>> byApplication(@PathVariable Long applicationId,
                                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.listByApplication(applicationId, access.actor(principal))));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InterviewDTO>>> my(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.myInterviews(access.actor(principal))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewDTO>> get(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getById(id, access.actor(principal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewDTO>> update(@PathVariable Long id,
                                                            @Valid @RequestBody InterviewDTO dto,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Interview updated",
                interviewService.update(id, dto, access.actor(principal))));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InterviewDTO>> cancel(@PathVariable Long id,
                                                            @Valid @RequestBody(required = false) ReasonRequest body,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Interview cancelled",
                interviewService.cancel(id, body == null ? null : body.reason(), access.actor(principal))));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InterviewDTO>> markStatus(@PathVariable Long id,
                                                                @Valid @RequestBody InterviewStatusRequest body,
                                                                @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Interview updated",
                interviewService.markStatus(id, body.status(), access.actor(principal))));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<ApiResponse<InterviewDTO>> feedback(@PathVariable Long id,
                                                              @Valid @RequestBody FeedbackRequest body,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted",
                interviewService.submitFeedback(id, body, access.actor(principal))));
    }
}
