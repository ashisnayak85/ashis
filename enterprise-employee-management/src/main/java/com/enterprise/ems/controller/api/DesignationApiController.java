package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.DesignationDTO;
import com.enterprise.ems.service.DesignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
@RequiredArgsConstructor
public class DesignationApiController {

    private final DesignationService designationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DesignationDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(designationService.getAllActive()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<DesignationDTO>>> getAllActiveSimple() {
        return ResponseEntity.ok(ApiResponse.success(designationService.getAllActiveSimple()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DesignationDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DesignationDTO>> create(@Valid @RequestBody DesignationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Designation created", designationService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DesignationDTO>> update(@PathVariable Long id, @Valid @RequestBody DesignationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Designation updated", designationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Designation deleted", null));
    }
}
