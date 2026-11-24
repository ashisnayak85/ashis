package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import com.enterprise.ems.repository.EmployeeRepository;
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

import java.util.HashMap;
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
    private final EmployeeRepository employeeRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        List<String> roles = user.getAuthorities().stream()
                .map(Object::toString)
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("username", user.getUsername());
        body.put("roles", roles);

        // Not every login has a linked Employee (e.g. a pure admin/IT account) -
        // these stay null and the frontend treats that as "no self-service views".
        employeeRepository.findByUserUsername(user.getUsername()).ifPresent(emp -> {
            body.put("employeeId", emp.getId());
            body.put("employeeName", emp.getFullName());
        });

        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails user) {
        userService.changePassword(user.getUsername(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    // Public, unauthenticated - this is exactly for users who are locked out and
    // can't log in. Always returns the same success message whether or not the
    // username/email actually matched an account (see UserServiceImpl for why).
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        userService.initiatePasswordReset(req.getUsernameOrEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account matches that username/email, a password reset link has been sent.", null));
    }

    // Public, unauthenticated - reached from the link inside the reset email.
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Please log in.", null));
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }

    @Data
    static class ForgotPasswordRequest {
        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;
    }

    @Data
    static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }
}
