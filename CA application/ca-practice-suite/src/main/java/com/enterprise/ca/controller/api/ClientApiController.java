package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.ClientDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientApiController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ClientDTO>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.search(name, clientType, active, PageRequest.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getAllActiveSimple() {
        return ResponseEntity.ok(ApiResponse.success(clientService.getAllActiveSimple()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clientService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientDTO>> create(@Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Client onboarded", clientService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDTO>> update(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Client updated", clientService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Client deactivated", null));
    }
}
