package com.enterprise.ems.controller.api;

import com.enterprise.ems.constant.OfferStatus;
import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.HiringRequests.ConvertRequest;
import com.enterprise.ems.dto.HiringRequests.ReasonRequest;
import com.enterprise.ems.dto.HiringRequests.RemarksRequest;
import com.enterprise.ems.dto.OfferDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.service.OfferService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/* PURPOSE: REST API for job offers: draft, approve, send, accept/decline, convert to employee, letter. */
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferApiController {

    private final OfferService offerService;
    private final HiringAccessService access;

    @PostMapping("/by-application/{applicationId}")
    public ResponseEntity<ApiResponse<OfferDTO>> create(@PathVariable Long applicationId,
                                                        @Valid @RequestBody OfferDTO dto,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Offer drafted",
                offerService.create(applicationId, dto, access.actor(principal))));
    }

    @GetMapping("/by-application/{applicationId}")
    public ResponseEntity<ApiResponse<List<OfferDTO>>> byApplication(@PathVariable Long applicationId,
                                                                    @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(offerService.listByApplication(applicationId, access.actor(principal))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<OfferDTO>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OfferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(offerService.search(keyword, status, pageable, access.actor(principal))));
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<OfferDTO>>> pendingApprovals(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(offerService.pendingApprovals(access.actor(principal))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDTO>> get(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(offerService.getById(id, access.actor(principal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDTO>> update(@PathVariable Long id,
                                                        @Valid @RequestBody OfferDTO dto,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer updated", offerService.update(id, dto, access.actor(principal))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<OfferDTO>> submit(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer submitted", offerService.submit(id, access.actor(principal))));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OfferDTO>> approve(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) RemarksRequest body,
                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer approved",
                offerService.approve(id, body == null ? null : body.remarks(), access.actor(principal))));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OfferDTO>> reject(@PathVariable Long id,
                                                        @Valid @RequestBody RemarksRequest body,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer rejected",
                offerService.reject(id, body.remarks(), access.actor(principal))));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<OfferDTO>> send(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer sent", offerService.send(id, access.actor(principal))));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<OfferDTO>> accept(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer marked as accepted", offerService.accept(id, access.actor(principal))));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<OfferDTO>> decline(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) ReasonRequest body,
                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer marked as declined",
                offerService.decline(id, body == null ? null : body.reason(), access.actor(principal))));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<OfferDTO>> revoke(@PathVariable Long id,
                                                        @Valid @RequestBody ReasonRequest body,
                                                        @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Offer revoked",
                offerService.revoke(id, body.reason(), access.actor(principal))));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<EmployeeDTO>> convert(@PathVariable Long id,
                                                            @Valid @RequestBody(required = false) ConvertRequest body,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Candidate converted to employee",
                offerService.convertToEmployee(id, body, access.actor(principal))));
    }

    // Printable offer letter (HTML). Opened in a new tab; the browser's Print dialog saves it as PDF.
    @GetMapping(value = "/{id}/letter", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> letter(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        String html = offerService.renderLetterHtml(id, access.actor(principal));
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(html);
    }
}
