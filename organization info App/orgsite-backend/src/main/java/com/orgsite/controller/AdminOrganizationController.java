package com.orgsite.controller;

import com.orgsite.dto.OrganizationDTO;
import com.orgsite.security.UserPrincipal;
import com.orgsite.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Everything here operates on the CALLER's own organization only - the org id
 * comes from the authenticated principal (derived from the JWT), never from the
 * request path or body. That's what keeps one owner from editing another's site.
 */
@RestController
@RequestMapping("/api/admin/organization")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<OrganizationDTO> getMyOrganization(@AuthenticationPrincipal UserPrincipal principal) {
        var org = organizationService.getById(principal.getOrganizationId());
        return ResponseEntity.ok(organizationService.toDTO(org));
    }

    @PutMapping
    public ResponseEntity<OrganizationDTO> updateMyOrganization(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @RequestBody OrganizationDTO dto) {
        var org = organizationService.update(principal.getOrganizationId(), dto);
        return ResponseEntity.ok(organizationService.toDTO(org));
    }

    @PatchMapping("/publish")
    public ResponseEntity<OrganizationDTO> setPublished(@AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam boolean published) {
        var org = organizationService.setPublished(principal.getOrganizationId(), published);
        return ResponseEntity.ok(organizationService.toDTO(org));
    }
}
