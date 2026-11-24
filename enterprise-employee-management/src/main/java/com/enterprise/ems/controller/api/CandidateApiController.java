package com.enterprise.ems.controller.api;

import com.enterprise.ems.constant.CandidateSource;
import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.CandidateDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.service.CandidateService;
import com.enterprise.ems.service.hiring.HiringAccessService;
import com.enterprise.ems.service.hiring.HiringActor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/*
 * PURPOSE: REST API for candidates, including the resume upload and the
 * AUTHENTICATED resume download (resumes are never served from /uploads).
 */
@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateApiController {

    private final CandidateService candidateService;
    private final HiringAccessService access;

    @PostMapping
    public ResponseEntity<ApiResponse<CandidateDTO>> create(@Valid @RequestBody CandidateDTO dto,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate created", candidateService.create(dto, access.actor(principal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateDTO>> update(@PathVariable Long id,
                                                            @Valid @RequestBody CandidateDTO dto,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Candidate updated",
                candidateService.update(id, dto, access.actor(principal))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CandidateDTO>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CandidateSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                candidateService.search(keyword, source, pageable, access.actor(principal))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateDTO>> get(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(candidateService.getById(id, access.actor(principal))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<CandidateDTO>> uploadResume(@PathVariable Long id,
                                                                  @RequestParam("file") MultipartFile file,
                                                                  @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded",
                candidateService.uploadResume(id, file, access.actor(principal))));
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails principal) {
        HiringActor actor = access.actor(principal);
        var download = candidateService.downloadResume(id, actor);
        boolean pdf = "application/pdf".equals(download.contentType());
        // PDFs open in the browser tab; Word files download. The file name goes through ContentDisposition
        // so it is encoded safely (no header injection from a hostile original file name).
        ContentDisposition disposition = (pdf ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @PostMapping("/{id}/anonymise")
    public ResponseEntity<ApiResponse<CandidateDTO>> anonymise(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Candidate data anonymised",
                candidateService.anonymise(id, access.actor(principal))));
    }
}
