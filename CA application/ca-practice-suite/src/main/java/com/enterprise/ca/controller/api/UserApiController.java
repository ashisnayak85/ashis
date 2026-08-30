package com.enterprise.ca.controller.api;

import com.enterprise.ca.dto.ApiResponse;
import com.enterprise.ca.dto.RoleDTO;
import com.enterprise.ca.dto.UserDTO;
import com.enterprise.ca.service.RoleService;
import com.enterprise.ca.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserApiController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody UserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created", userService.createUser(dto)));
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<ApiResponse<Void>> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.setEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(enabled ? "User enabled" : "User disabled", null));
    }
}
