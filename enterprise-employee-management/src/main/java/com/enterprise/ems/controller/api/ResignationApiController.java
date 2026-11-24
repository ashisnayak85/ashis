package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.dto.ResignationAssetDTO;
import com.enterprise.ems.dto.ResignationDTO;
import com.enterprise.ems.security.CurrentEmployeeResolver;
import com.enterprise.ems.service.ResignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/*
 * PURPOSE: Resignation / separation module REST API.
 * See ResignationServiceImpl for the full state machine.
 */
@RestController
@RequestMapping("/api/resignations")
@RequiredArgsConstructor
public class ResignationApiController {

    private final ResignationService resignationService;
    private final CurrentEmployeeResolver currentEmployeeResolver;

    // --- Employee self-service ---

    public record SubmitRequest(String reason, Integer noticePeriodDays) {}

    @PostMapping
    public ResponseEntity<ApiResponse<ResignationDTO>> submit(
            @RequestBody SubmitRequest body,
            @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Resignation submitted",
                resignationService.submit(employee.getId(), body.reason(), body.noticePeriodDays())));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<ResignationDTO>> getMyLatest(@AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(resignationService.getMyLatest(employee.getId())));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<ResignationDTO>> withdraw(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Resignation withdrawn", resignationService.withdraw(id, employee.getId())));
    }

    // --- Viewing ---

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResignationDTO>> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        Long employeeId = resolveOptionalEmployeeId(principal);
        boolean privileged = currentEmployeeResolver.isPrivileged(principal);
        return ResponseEntity.ok(ApiResponse.success(resignationService.getById(id, employeeId, privileged)));
    }

    // Admin/HR: every resignation, filterable by status.
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<ResignationDTO>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(resignationService.search(status, PageRequest.of(page, size))));
    }

    // --- Approver inbox / actions ---

    // Open to any authenticated employee - being the resolved approver on a
    // pending stage IS the authorization check, done inside the service.
    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<ResignationDTO>>> getPendingApprovalsForMe(@AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(resignationService.getPendingApprovalsForMe(employee.getId())));
    }

    public record ActionRequest(String remarks) {}

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ResignationDTO>> approveStage(
            @PathVariable Long id, @RequestBody(required = false) ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        String remarks = body != null ? body.remarks() : null;
        return ResponseEntity.ok(ApiResponse.success("Stage approved", resignationService.approveStage(id, employee.getId(), remarks)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ResignationDTO>> rejectStage(
            @PathVariable Long id, @RequestBody ActionRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Resignation rejected", resignationService.rejectStage(id, employee.getId(), body.remarks())));
    }

    // --- No-dues clearance (department-side action) ---

    public record ClearanceRequest(Long departmentId, String remarks) {}

    @PostMapping("/{id}/clearance/clear")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResignationDTO>> clearDepartment(
            @PathVariable Long id, @RequestBody ClearanceRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Department cleared",
                resignationService.clearDepartment(id, body.departmentId(), employee.getId(), body.remarks())));
    }

    @PostMapping("/{id}/clearance/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResignationDTO>> blockDepartment(
            @PathVariable Long id, @RequestBody ClearanceRequest body, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Department clearance blocked",
                resignationService.blockDepartment(id, body.departmentId(), employee.getId(), body.remarks())));
    }

    // --- Asset return line items ---

    public record AssetRequest(String assetDescription) {}

    @PostMapping("/{id}/assets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResignationAssetDTO>> addAsset(@PathVariable Long id, @RequestBody AssetRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset item added", resignationService.addAsset(id, body.assetDescription())));
    }

    public record AssetReturnRequest(String remarks) {}

    @PostMapping("/{id}/assets/{assetId}/returned")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResignationDTO>> markAssetReturned(
            @PathVariable Long id, @PathVariable Long assetId, @RequestBody(required = false) AssetReturnRequest body) {
        String remarks = body != null ? body.remarks() : null;
        return ResponseEntity.ok(ApiResponse.success("Asset marked returned", resignationService.markAssetReturned(id, assetId, remarks)));
    }

    // --- Final approval / last-working-day override ---

    @PostMapping("/{id}/final-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResignationDTO>> finalApprove(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success("Resignation finally approved - employee relieved", resignationService.finalApprove(id, employee.getId())));
    }

    public record OverrideLwdRequest(LocalDate actualLastWorkingDay, String overrideReason) {}

    @PostMapping("/{id}/override-lwd")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResignationDTO>> overrideLastWorkingDay(@PathVariable Long id, @RequestBody OverrideLwdRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Last working day updated",
                resignationService.overrideLastWorkingDay(id, body.actualLastWorkingDay(), body.overrideReason())));
    }

    private Long resolveOptionalEmployeeId(UserDetails principal) {
        try {
            return currentEmployeeResolver.requireCurrentEmployee(principal).getId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
