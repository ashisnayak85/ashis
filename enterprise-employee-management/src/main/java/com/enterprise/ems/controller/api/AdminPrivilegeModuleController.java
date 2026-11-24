package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.PrivilegeModuleDTO;
import com.enterprise.ems.service.PrivilegeModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: Admin-only view/toggle of the module master, backing the module
 * dropdown on the unified Workflow Setup screen. Deliberately has NO create
 * endpoint - see PrivilegeModuleService javadoc. New modules are registered
 * automatically by the app on startup (PrivilegeModuleInitializer), never
 * typed in by admin, because a module's code is a hardcoded contract with
 * the Java service that filters by it.
 */
@RestController
@RequestMapping("/api/admin/privilege-modules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPrivilegeModuleController {

    private final PrivilegeModuleService privilegeModuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrivilegeModuleDTO>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(privilegeModuleService.getAll(activeOnly)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<PrivilegeModuleDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Module deactivated", privilegeModuleService.deactivate(id)));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<PrivilegeModuleDTO>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Module reactivated", privilegeModuleService.reactivate(id)));
    }
}
