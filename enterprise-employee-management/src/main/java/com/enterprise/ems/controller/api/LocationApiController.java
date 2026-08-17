package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.dto.LocationDTO;
import com.enterprise.ems.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllActive()));
    }

    // Lightweight list for dropdowns (employee form, filters) - no employee-count query per row.
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getAllActiveSimple() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllActiveSimple()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LocationDTO>> create(@Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Location created", locationService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LocationDTO>> update(@PathVariable Long id, @Valid @RequestBody LocationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Location updated", locationService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Location deleted", null));
    }
}
