package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.ComplianceTaskDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.service.ComplianceTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceApiController {

    private final ComplianceTaskService complianceTaskService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ComplianceTaskDTO>>> search(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(complianceTaskService.search(
                clientId, status, taskType, PageRequest.of(page, size, Sort.by("dueDate")))));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<ComplianceTaskDTO>>> upcoming(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(complianceTaskService.getUpcoming(limit)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ComplianceTaskDTO>> create(@Valid @RequestBody ComplianceTaskDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Compliance task scheduled", complianceTaskService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplianceTaskDTO>> update(@PathVariable Long id, @Valid @RequestBody ComplianceTaskDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Compliance task updated", complianceTaskService.update(id, dto)));
    }

    @PutMapping("/{id}/file")
    public ResponseEntity<ApiResponse<ComplianceTaskDTO>> markFiled(@PathVariable Long id,
                                                                      @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Marked as filed", complianceTaskService.markFiled(id, remarks)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        complianceTaskService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Compliance task deleted", null));
    }
}
