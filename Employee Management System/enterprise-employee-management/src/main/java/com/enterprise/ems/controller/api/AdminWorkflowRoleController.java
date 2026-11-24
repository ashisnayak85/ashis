package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.WorkflowRoleDTO;
import com.enterprise.ems.service.WorkflowRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: Admin-only "Role Master" screen. Business/workflow roles (HOD,
 * HR Head, Project Head, ...) used for approval routing - completely
 * separate from Spring Security's auth Role/User. Admin creates, renames,
 * and retires roles here with no programmer/redeploy involved. See
 * WorkflowRole entity javadoc.
 */
@RestController
@RequestMapping("/api/admin/workflow-roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkflowRoleController {

    private final WorkflowRoleService workflowRoleService;

    public record WorkflowRoleRequest(String name, String description) {}

    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowRoleDTO>> create(@RequestBody WorkflowRoleRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workflow role created", workflowRoleService.create(body.name(), body.description())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkflowRoleDTO>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(workflowRoleService.getAll(activeOnly)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowRoleDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workflowRoleService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowRoleDTO>> update(@PathVariable Long id, @RequestBody WorkflowRoleRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Workflow role updated", workflowRoleService.update(id, body.name(), body.description())));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WorkflowRoleDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Workflow role deactivated", workflowRoleService.deactivate(id)));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<WorkflowRoleDTO>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Workflow role reactivated", workflowRoleService.reactivate(id)));
    }
}
