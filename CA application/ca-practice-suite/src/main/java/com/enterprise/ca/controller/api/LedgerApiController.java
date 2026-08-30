package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.LedgerEntryDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.service.LedgerEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerApiController {

    private final LedgerEntryService ledgerEntryService;

    @PostMapping
    public ResponseEntity<ApiResponse<LedgerEntryDTO>> post(@Valid @RequestBody LedgerEntryDTO dto,
                                                              @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Entry posted", ledgerEntryService.post(dto, principal.getUsername())));
    }

    @PutMapping("/{id}/reconcile")
    public ResponseEntity<ApiResponse<LedgerEntryDTO>> toggleReconciled(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ledgerEntryService.toggleReconciled(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ledgerEntryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Entry deleted", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<LedgerEntryDTO>>> search(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Boolean reconciled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(ledgerEntryService.search(
                clientId, accountType, start, end, reconciled,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "entryDate")))));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Boolean reconciled) {
        byte[] xlsx = ledgerEntryService.exportSearch(clientId, accountType, start, end, reconciled);
        String filename = "ledger-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
