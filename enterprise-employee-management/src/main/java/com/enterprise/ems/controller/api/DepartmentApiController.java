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
    private final com.enterprise.ems.service.FaceRecognitionService faceRecognitionService;
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
    // Split into punch-in/punch-out rather than a free-form form: the server
    // clock decides the time, and the flow enforces you can't punch in twice
    // or punch out before punching in.
    //
    // Accepts multipart/form-data (not JSON) so the same endpoint works
    // whether or not face verification is switched on: "remarks" is always
    // optional text, "image" is an optional captured photo that is REQUIRED
    // and CHECKED only when attendance.face-verification.enabled=true (see
    // FaceRecognitionService#verifyIfRequired). With the flag off, "image" is
    // simply ignored and this behaves exactly like the old JSON-only version.
    @PostMapping(value = "/self/punch-in", consumes = {
            org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE,
            org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ResponseEntity<ApiResponse<AttendanceDTO>> punchIn(
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        // Throws BusinessException (-> 400) if face verification is on and the
        // photo is missing/doesn't match - punch-in never proceeds in that case.
        boolean faceVerified = faceRecognitionService.verifyIfRequired(employee.getId(), image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Punched in", attendanceService.punchIn(employee.getId(), remarks, faceVerified)));
    }

    @PostMapping(value = "/self/punch-out", consumes = {
            org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE,
            org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ResponseEntity<ApiResponse<AttendanceDTO>> punchOut(
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        boolean faceVerified = faceRecognitionService.verifyIfRequired(employee.getId(), image);
        return ResponseEntity.ok(ApiResponse.success("Punched out", attendanceService.punchOut(employee.getId(), remarks, faceVerified)));
    }

    // Tells the frontend whether to show the camera step at all (enabled) and
    // whether THIS employee still needs to enroll their face first (enrolled).
    // The single source of truth for "is this feature on" - the React app
    // never hardcodes this, it always asks the server.
    @GetMapping("/self/face/status")
    public ResponseEntity<ApiResponse<com.enterprise.ems.dto.FaceStatusDTO>> faceStatus(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        boolean enabled = faceRecognitionService.isEnabled();
        boolean enrolled = enabled && faceRecognitionService.hasEnrollment(employee.getId());
        return ResponseEntity.ok(ApiResponse.success(
                com.enterprise.ems.dto.FaceStatusDTO.builder().enabled(enabled).enrolled(enrolled).build()));
    }

    // One-time (or re-doable) capture of the employee's own reference face.
    // Same "resolve from session" pattern as punch-in/out - an employee can
    // only ever enroll their own face here, never someone else's.
    @PostMapping(value = "/self/face/enroll", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> enrollFace(
            @RequestParam("image") MultipartFile image,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        faceRecognitionService.enroll(employee.getId(), image, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Face enrolled successfully", null));
    }

    // Today's status for the logged-in employee - null data means "not punched
    // in yet today", which the frontend uses to decide which button to show.
    @GetMapping("/self/today")
    public ResponseEntity<ApiResponse<AttendanceDTO>> myTodayStatus(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getTodayStatus(employee.getId())));
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

    // Same filter + self-scoping as /search above, but returns the full
    // matching set as a downloadable .xlsx instead of a paginated JSON page.
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String status,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        Long scopedEmployeeId = employeeId;
        if (!currentEmployeeResolver.isPrivileged(principal)) {
            scopedEmployeeId = currentEmployeeResolver.requireCurrentEmployee(principal).getId();
        }
        byte[] xlsx = attendanceService.exportAttendance(scopedEmployeeId, startDate, endDate, status);
        String filename = "attendance-" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}

/*
 * ================================================================================
 * PURPOSE: Admin/Manager face-enrollment management - "for all the employees",
 * not just the self-service enrollment an employee does for themselves.
 * ================================================================================
 * Lets HR/admin (re-)enroll ANY employee's face (e.g. onboarding someone who
 * can't do it themselves yet, or fixing a bad first capture), check whether a
 * given employee is enrolled, review the backup history of past enrollments,
 * and - the diagnostic tool this exists for - run a real photo against an
 * employee's enrolled face and see the ACTUAL similarity score, not just a
 * pass/fail, to figure out why real punch-ins are being rejected.
 * ================================================================================
 */
@RestController
@RequestMapping("/api/attendance/admin/face")
@RequiredArgsConstructor
class FaceAdminApiController {

    private final com.enterprise.ems.service.FaceRecognitionService faceRecognitionService;

    @GetMapping("/status/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<com.enterprise.ems.dto.FaceStatusDTO>> status(@PathVariable Long employeeId) {
        boolean enabled = faceRecognitionService.isEnabled();
        boolean enrolled = enabled && faceRecognitionService.hasEnrollment(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                com.enterprise.ems.dto.FaceStatusDTO.builder().enabled(enabled).enrolled(enrolled).build()));
    }

    // Admin (re-)enrolling ANY employee's face. If this employee already has
    // an enrollment, the previous one is automatically backed up (see
    // FaceRecognitionService#enroll) before being overwritten - nothing is lost.
    @PostMapping(value = "/enroll/{employeeId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> enroll(
            @PathVariable Long employeeId,
            @RequestParam("image") MultipartFile image,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        faceRecognitionService.enroll(employeeId, image, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Face enrolled successfully", null));
    }

    // The diagnostic "observe" tool: runs the same comparison the real
    // punch-in uses, but returns the actual similarity score and threshold
    // instead of just throwing on a mismatch - use this to see WHY a
    // particular employee is failing verification (borderline near-miss vs.
    // wildly off) rather than guessing from server logs.
    @PostMapping(value = "/test-verify/{employeeId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<com.enterprise.ems.dto.FaceVerifyResultDTO>> testVerify(
            @PathVariable Long employeeId,
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success(faceRecognitionService.testVerify(employeeId, image)));
    }

    @GetMapping("/history/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<com.enterprise.ems.dto.FaceEnrollmentHistoryDTO>>> history(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(faceRecognitionService.getHistory(employeeId)));
    }
}

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
class LeaveApiController {

    private final LeaveService leaveService;
    private final com.enterprise.ems.security.CurrentEmployeeResolver currentEmployeeResolver;
    private final com.enterprise.ems.service.AuditService auditService;

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

    // Admin/manager: search across all employees' leave records.
    // All filters optional - status, date range (overlap - see LeaveSpecifications),
    // and employee name. Replaces the old "/pending"-only view for the main leave
    // requests screen; "/pending" above is left in place in case anything else
    // still depends on it.
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<LeaveDTO>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate to,
            @RequestParam(required = false) String employeeName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.searchLeaves(status, from, to, employeeName, PageRequest.of(page, size))));
    }

    // Self-service: "my leave history", optionally filtered by status
    // (PENDING/APPROVED/REJECTED) and/or date range (overlap - see
    // LeaveSpecifications). Employee is always resolved from the session -
    // never trust an employeeId supplied by the client here.
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<LeaveDTO>>> getMyLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.getMyLeaves(employee.getId(), status, from, to, PageRequest.of(page, size))));
    }

    // Admin/manager: same filters as /search, but returns the full filtered
    // result set as a downloadable .xlsx instead of a paginated JSON page.
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportSearch(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate to,
            @RequestParam(required = false) String employeeName,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        byte[] xlsx = leaveService.exportLeaves(null, status, from, to, employeeName);
        auditService.log("EXPORT", "Leave", null, "Leave records exported by: " + principal.getUsername());
        return excelResponse(xlsx, "leave-requests");
    }

    // Self-service: export the logged-in employee's own leave history. Same
    // status/date-range filters as /my, employee always resolved server-side.
    @GetMapping("/my/export")
    public ResponseEntity<byte[]> exportMyLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate to,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails principal) {
        var employee = currentEmployeeResolver.requireCurrentEmployee(principal);
        byte[] xlsx = leaveService.exportLeaves(employee.getId(), status, from, to, null);
        auditService.log("EXPORT", "Leave", employee.getId(), "Own leave history exported by: " + principal.getUsername());
        return excelResponse(xlsx, "my-leave-history");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] xlsx, String filenamePrefix) {
        String filename = filenamePrefix + "-" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
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
