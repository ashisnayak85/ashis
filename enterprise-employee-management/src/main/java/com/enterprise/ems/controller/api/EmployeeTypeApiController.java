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
public class EmployeeTypeApiController {


    private final EmployeeTypeService employeeTypeService;

    @GetMapping("/employeeTypes")
    public ResponseEntity<ApiResponse<List<EmployeeTypeDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(employeeTypeService.getAllActive()));
    }
    
    @GetMapping("/employeeTypes/active")
    public ResponseEntity<ApiResponse<List<EmployeeTypeDTO>>> getAllActiveSimple() {
        return ResponseEntity.ok(ApiResponse.success(employeeTypeService.getAllActiveSimple()));
    }

//    @GetMapping("/employeeTypes/{id}")
//    public ResponseEntity<ApiResponse<EmployeeTypeDTO>> getById(@PathVariable Long id) {
//        return ResponseEntity.ok(ApiResponse.success(employeeTypeService.getById(id)));
//    }

    @PostMapping("/employeeTypes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeTypeDTO>> create(@Valid @RequestBody EmployeeTypeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee Type created", employeeTypeService.create(dto)));
    }

    @PutMapping("/employeeTypes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeTypeDTO>> update(@PathVariable Long id, @Valid @RequestBody EmployeeTypeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Employee Type updated", employeeTypeService.update(id, dto)));
    }

    @DeleteMapping("/employeeTypes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employee Type deleted", null));
    }

}
