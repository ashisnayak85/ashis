package com.enterprise.ems.controller.api;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.*;
import com.enterprise.ems.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
 * ================================================================================
 * PURPOSE: REST API Controller for Employee (Phase 8)
 * ================================================================================
 * ANNOTATION: @RestController = @Controller + @ResponseBody (returns JSON, not view)
 *
 * ACCESS: this is org-wide employee data (salary, personal details, everyone's
 * records) - restricted to ADMIN/MANAGER throughout. A plain employee (ROLE_USER)
 * never calls this controller; their own data comes back through the /my-* self-
 * service endpoints on Dashboard/Leave/Attendance instead.
 *
 * AJAX FLOW:
 * Browser JS -> $.ajax() -> POST /api/employees -> this controller ->
 * EmployeeService -> JSON ApiResponse -> browser updates DOM
 * ================================================================================
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeApiController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("firstName"));
        PageResponse<EmployeeDTO> result = (search != null && !search.isBlank())
                ? employeeService.search(search, pageable)
                : employeeService.getAll(pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    // Active employees with no login yet - populates the "New User" picker so an admin
    // can only grant access to a real, active employee, never a free-typed account.
    @GetMapping("/available-for-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<EmployeeDTO>>> getAvailableForUser() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAvailableForUserCreation()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> create(@Valid @RequestBody EmployeeDTO dto) {
        EmployeeDTO created = employeeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", employeeService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted", null));
    }
}
