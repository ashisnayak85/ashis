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
import org.springframework.web.bind.annotation.*;

/*
 * ================================================================================
 * PURPOSE: REST API Controller for Employee (Phase 8)
 * ================================================================================
 * ANNOTATION: @RestController = @Controller + @ResponseBody (returns JSON, not view)
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
    public ResponseEntity<ApiResponse<EmployeeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    // Active employees with no login yet - populates the "New User" picker so an admin
    // can only grant access to a real, active employee, never a free-typed account.
    @GetMapping("/available-for-user")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<EmployeeDTO>>> getAvailableForUser() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAvailableForUserCreation()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDTO>> create(@Valid @RequestBody EmployeeDTO dto) {
        EmployeeDTO created = employeeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", employeeService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted", null));
    }
}
