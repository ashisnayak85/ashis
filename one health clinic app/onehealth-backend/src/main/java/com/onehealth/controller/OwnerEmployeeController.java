package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OWNER-only: HR data (gender/DOB/joining date/addresses/salary history) for
 * clinic-authority and doctor staff - kept as its own controller, separate
 * from OwnerController's operational clinic/doctor management, since this is
 * a distinct concern (HR) with more sensitive data (salary). Every response
 * shape here is owner-only - never reused on a DoctorDTO or anything another
 * role's endpoints return.
 */
@RestController
@RequestMapping("/api/owner/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeListItemDTO>> list() {
        return ResponseEntity.ok(employeeService.listEmployees(CurrentUser.organizationId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<EmployeeProfileDTO> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(employeeService.getProfile(CurrentUser.organizationId(), userId));
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<EmployeeProfileDTO> upsertProfile(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateEmployeeProfileRequest req) {
        return ResponseEntity.ok(employeeService.upsertProfile(CurrentUser.organizationId(), userId, req));
    }

    /** Appends a new salary revision - always additive, see SalaryRecord javadoc. */
    @PostMapping("/{userId}/salary")
    public ResponseEntity<EmployeeProfileDTO> addSalary(@PathVariable Long userId,
                                                         @Valid @RequestBody AddSalaryRecordRequest req) {
        return ResponseEntity.ok(employeeService.addSalaryRecord(CurrentUser.organizationId(), userId, req));
    }
}
