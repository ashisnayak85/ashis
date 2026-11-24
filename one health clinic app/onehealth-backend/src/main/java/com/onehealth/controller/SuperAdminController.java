package com.onehealth.controller;

import com.onehealth.dto.CreateOrganizationRequest;
import com.onehealth.entity.Organization;
import com.onehealth.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Platform-operator only: onboarding new clinic-chain customers (new Organizations). */
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @PostMapping("/organizations")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody CreateOrganizationRequest req) {
        return ResponseEntity.ok(superAdminService.createOrganization(req));
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<Organization>> listOrganizations() {
        return ResponseEntity.ok(superAdminService.listOrganizations());
    }

    @PutMapping("/organizations/{id}/status")
    public ResponseEntity<Organization> setStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(superAdminService.setOrganizationActive(id, active));
    }
}
