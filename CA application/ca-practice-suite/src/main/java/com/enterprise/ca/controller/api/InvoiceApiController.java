package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.InvoiceDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceApiController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InvoiceDTO>>> search(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.search(
                clientId, type, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceDate")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDTO>> create(@Valid @RequestBody InvoiceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invoice created", invoiceService.create(dto)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InvoiceDTO>> updateStatus(@PathVariable Long id,
                                                                  @RequestParam String status,
                                                                  @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Invoice status updated",
                invoiceService.updateStatus(id, status, principal.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
    }
}
