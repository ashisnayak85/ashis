package com.enterprise.ems.controller.api;

import com.enterprise.ems.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * ================================================================================
 * PURPOSE: Session/identity check for the React SPA.
 * ================================================================================
 * The SPA calls this once on load (and after login) to find out whether the
 * existing session cookie is still valid, and what roles the user has - so it
 * can decide what to render without guessing from a JWT payload (we don't use one).
 * ================================================================================
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

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
}
