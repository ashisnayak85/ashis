package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * ================================================================================
 * PURPOSE: REST API backing allEmployeeList.html / allEmployee.js
 * ================================================================================
 * Unlike /api/employees (EmployeeApiController), which only returns active
 * employees, this endpoint returns EVERY row in the Employee table -
 * i.e. "SELECT * FROM Employee" - via EmployeeRepository#findAll(Pageable).
 * Org-wide data -> admin/manager only.
 * ================================================================================
 */
@RestController
@RequestMapping("/api/allEmployees")
@RequiredArgsConstructor
public class AllEmployeeApiController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("firstName"));
        PageResponse<EmployeeDTO> result = (search != null && !search.isBlank())
                ? employeeService.searchAllEmployees(search, pageable)
                : employeeService.getAllEmployees(pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
