package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * ================================================================================
 * PURPOSE: Session/identity check for the React SPA, plus self-service password change.
 * ================================================================================
 * /me: The SPA calls this once on load (and after login) to find out whether the
 * existing session cookie is still valid, and what roles the user has.
 *
 * /change-password: works for BOTH admin and employee roles - a user always changes
 * their own password, identified from their session, never by id.
 * ================================================================================
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        List<String> roles = user.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "username", user.getUsername(),
                "roles", roles
        )));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails user) {
        userService.changePassword(user.getUsername(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }
}
