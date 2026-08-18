package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DepartmentApiController {

    private final DepartmentService departmentService;

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getAllActive()));
    }
    
    @GetMapping("/departments/active")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllActiveSimple() {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getAllActiveSimple()));
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getById(id)));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> create(@Valid @RequestBody DepartmentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created", departmentService.create(dto)));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> update(@PathVariable Long id, @Valid @RequestBody DepartmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Department updated", departmentService.update(id, dto)));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted", null));
    }
}

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
class AttendanceApiController {

    private final AttendanceService attendanceService;
    private final com.enterprise.ems.security.CurrentEmployeeResolver currentEmployeeResolver;

    // Shared secret the biometric device sends as X-Device-Key. Set BIOMETRIC_API_KEY
    // in the environment for real deployments - see application.properties.
    @org.springframework.beans.factory.annotation.Value("${biometric.api-key:}")
    private String biometricApiKey;

    // Admin/Manager marking or correcting attendance for ANY employee, any date.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceDTO>> mark(@Valid @RequestBody AttendanceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance marked", attendanceService.markAttendance(dto)));
    }

    // Employee marking their OWN attendance for today. Available to any
    // authenticated user - the employee is resolved from the session, never
    // from the request body, so this can't be used to mark for someone else.
    @PostMapping("/self")
    public ResponseEntity<ApiResponse<AttendanceDTO>> markSelf(
            @Valid @RequestBody AttendanceDTO dto,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance marked", attendanceService.markSelfAttendance(employee.getId(), dto)));
    }

    // Ingestion endpoint for the biometric machine. Not a user session - the
    // device authenticates with a shared API key (see BiometricDeviceAuthFilter /
    // application.properties: biometric.api-key). Permitted anonymously at the
    // Spring Security layer and gated by the key check inside this method.
    @PostMapping("/biometric")
    public ResponseEntity<ApiResponse<AttendanceDTO>> biometricPunch(
            @Valid @RequestBody BiometricPunchDTO dto,
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey) {
        if (biometricApiKey == null || biometricApiKey.isBlank() || !biometricApiKey.equals(deviceKey)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid or missing device key"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Punch recorded", attendanceService.recordBiometricPunch(dto)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDTO>>> getByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getByEmployee(employeeId, PageRequest.of(page, size))));
    }

    // Combined filter used by the "View Attendance" panel: employee, date range,
    // and attendance type are all optional and can be mixed freely. A plain
    // ROLE_USER always gets forced to their own employeeId here, regardless of
    // what was passed in - so this same endpoint safely doubles as "my history".
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDTO>>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        Long scopedEmployeeId = employeeId;
        if (!currentEmployeeResolver.isPrivileged(principal)) {
            scopedEmployeeId = currentEmployeeResolver.requireCurrentEmployee(principal).getId();
        }
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.search(scopedEmployeeId, startDate, endDate, status, PageRequest.of(page, size))));
    }
}

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
class LeaveApiController {

    private final LeaveService leaveService;
    private final com.enterprise.ems.security.CurrentEmployeeResolver currentEmployeeResolver;

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveDTO>> apply(
            @Valid @RequestBody LeaveDTO dto,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        // A plain employee can only ever apply for themselves - the employeeId they
        // sent (if any) is ignored and replaced with their own. ADMIN/MANAGER keep
        // the ability to file leave on behalf of whichever employee they picked.
        if (!currentEmployeeResolver.isPrivileged(principal)) {
            dto.setEmployeeId(currentEmployeeResolver.requireCurrentEmployee(principal).getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave applied", leaveService.applyLeave(dto)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Leave approved",
                leaveService.approveLeave(id, "manager")));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveDTO>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Leave rejected",
                leaveService.rejectLeave(id, "manager")));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveDTO>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getPendingLeaves()));
    }

    // Self-service: "my leave history", optionally filtered by status
    // (PENDING/APPROVED/REJECTED). Employee is always resolved from the session.
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<LeaveDTO>>> getMyLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.getMyLeaves(employee.getId(), status, PageRequest.of(page, size))));
    }
}

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
class UserApiController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    // Frontend fetches the real, current roles from the DB instead of hardcoding
    // role names - so the "New User" form dropdown can never drift out of sync
    // with what's actually seeded/configured in the roles table.
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody UserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", userService.createUser(dto)));
    }
}

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
class FileApiController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        var result = fileStorageService.storeFile(file, entityType, entityId, "user");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded", result));
    }

    // Lists whatever's been uploaded for one entity (e.g. an employee's
    // qualification certificate) - used to show "current file" when editing.
    @GetMapping
    public ResponseEntity<ApiResponse<List<com.enterprise.ems.entity.FileUpload>>> list(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(fileStorageService.getFiles(entityType, entityId)));
    }

    // Streams the raw file back so the browser can open/download it
    // (e.g. clicking "View certificate" on the employee form).
    @GetMapping("/{id}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id) throws java.io.IOException {
        var meta = fileStorageService.getFile(id);
        var path = java.nio.file.Paths.get(meta.getFilePath());
        var resource = new org.springframework.core.io.UrlResource(path.toUri());
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        meta.getFileType() != null ? meta.getFileType() : "application/octet-stream"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalFilename() + "\"")
                .body(resource);
    }
}

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
class DashboardApiController {

    private final DashboardService dashboardService;
    private final com.enterprise.ems.security.CurrentEmployeeResolver currentEmployeeResolver;

    @GetMapping("/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }

    // Personal dashboard for a plain ROLE_USER login - always scoped to whoever
    // is actually signed in, never to an id supplied by the caller.
    @GetMapping("/my-stats")
    public ResponseEntity<ApiResponse<MyDashboardStats>> getMyStats(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getMyStats(employee.getId(), employee.getFullName())));
    }
}
