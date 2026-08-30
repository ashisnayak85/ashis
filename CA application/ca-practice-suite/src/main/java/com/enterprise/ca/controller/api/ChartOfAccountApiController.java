package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.ChartOfAccountDTO;
import com.enterprise.ca.service.ChartOfAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class ChartOfAccountApiController {

    private final ChartOfAccountService chartOfAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChartOfAccountDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(chartOfAccountService.getAllActive()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChartOfAccountDTO>> create(@Valid @RequestBody ChartOfAccountDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Ledger head created", chartOfAccountService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChartOfAccountDTO>> update(@PathVariable Long id, @Valid @RequestBody ChartOfAccountDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Ledger head updated", chartOfAccountService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        chartOfAccountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Ledger head deactivated", null));
    }
}
