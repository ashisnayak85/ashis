package com.orgsite.controller;

import com.orgsite.dto.UploadResponse;
import com.orgsite.security.UserPrincipal;
import com.orgsite.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, principal.getOrganizationId());
        return ResponseEntity.ok(UploadResponse.builder().url(url).build());
    }
}
