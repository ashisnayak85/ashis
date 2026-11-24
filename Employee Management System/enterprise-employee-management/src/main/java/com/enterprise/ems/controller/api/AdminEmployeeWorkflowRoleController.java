package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.EmployeeWorkflowRoleDTO;
import com.enterprise.ems.security.CurrentEmployeeResolver;
import com.enterprise.ems.service.EmployeeWorkflowRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * PURPOSE: Admin-only "Employee Access" screen - give an employee a
 * WorkflowRole (optionally scoped to a department), or take it away. This is
 * the data ResignationServiceImpl.resolveApprover() reads at submit time -
 * see EmployeeWorkflowRole entity javadoc. Admin operates this entirely
 * through the API; no programmer involvement needed to change who approves
 * what.
 */
@RestController
@RequestMapping("/api/admin/employee-roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeWorkflowRoleController {

    private final EmployeeWorkflowRoleService employeeWorkflowRoleService;
    private final CurrentEmployeeResolver currentEmployeeResolver;

    // scopeDepartmentId null = global/company-wide hold of the role.
    public record AssignRequest(Long employeeId, Long workflowRoleId, Long scopeDepartmentId) {}

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeWorkflowRoleDTO>> assign(
            @RequestBody AssignRequest body, @AuthenticationPrincipal UserDetails principal) {
        Long assignedByEmployeeId = resolveOptionalEmployeeId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Role assigned",
                employeeWorkflowRoleService.assign(body.employeeId(), body.workflowRoleId(), body.scopeDepartmentId(), assignedByEmployeeId)));
    }

    @PatchMapping("/{assignmentId}/revoke")
    public ResponseEntity<ApiResponse<EmployeeWorkflowRoleDTO>> revoke(
            @PathVariable Long assignmentId, @AuthenticationPrincipal UserDetails principal) {
        Long removedByEmployeeId = resolveOptionalEmployeeId(principal);
        return ResponseEntity.ok(ApiResponse.success("Role removed", employeeWorkflowRoleService.revoke(assignmentId, removedByEmployeeId)));
    }

    @GetMapping("/by-employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeWorkflowRoleDTO>>> getForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(employeeWorkflowRoleService.getForEmployee(employeeId)));
    }

    @GetMapping("/by-role/{workflowRoleId}")
    public ResponseEntity<ApiResponse<List<EmployeeWorkflowRoleDTO>>> getForRole(@PathVariable Long workflowRoleId) {
        return ResponseEntity.ok(ApiResponse.success(employeeWorkflowRoleService.getForRole(workflowRoleId)));
    }

    private Long resolveOptionalEmployeeId(UserDetails principal) {
        try {
            return currentEmployeeResolver.requireCurrentEmployee(principal).getId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
