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

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceDTO>> mark(@Valid @RequestBody AttendanceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance marked", attendanceService.markAttendance(dto)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDTO>>> getByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getByEmployee(employeeId, PageRequest.of(page, size))));
    }
}

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
class LeaveApiController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveDTO>> apply(@Valid @RequestBody LeaveDTO dto) {
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

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }
}
