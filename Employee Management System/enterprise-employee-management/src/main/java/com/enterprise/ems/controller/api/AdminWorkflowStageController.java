package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.WorkflowStageDTO;
import com.enterprise.ems.service.WorkflowStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: Admin-only "Approval Structure Master" - now module-scoped.
 * Renamed/generalized from AdminResignationStageController: every endpoint
 * takes a privilegeModuleId (path or body) so the SAME controller (and the
 * SAME admin screen, with a module dropdown) covers Resignation today and
 * any future module without new code or a new sidebar entry. See
 * PrivilegeModule / WorkflowStage entity javadocs.
 */
@RestController
@RequestMapping("/api/admin/workflow-stages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkflowStageController {

    private final WorkflowStageService stageService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowStageDTO>> create(@RequestBody WorkflowStageDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Approval stage created", stageService.create(body)));
    }

    @GetMapping("/by-module/{privilegeModuleId}")
    public ResponseEntity<ApiResponse<List<WorkflowStageDTO>>> getForModule(@PathVariable Long privilegeModuleId) {
        return ResponseEntity.ok(ApiResponse.success(stageService.getForModule(privilegeModuleId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowStageDTO>> update(
            @PathVariable Long id, @RequestBody WorkflowStageDTO body) {
        return ResponseEntity.ok(ApiResponse.success("Approval stage updated", stageService.update(id, body)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WorkflowStageDTO>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Approval stage deactivated", stageService.deactivate(id)));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<WorkflowStageDTO>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Approval stage reactivated", stageService.reactivate(id)));
    }

    public record ReorderRequest(Long privilegeModuleId, List<Long> orderedStageIds) {}

    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<List<WorkflowStageDTO>>> reorder(@RequestBody ReorderRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Approval chain reordered",
                stageService.reorder(body.privilegeModuleId(), body.orderedStageIds())));
    }
}
