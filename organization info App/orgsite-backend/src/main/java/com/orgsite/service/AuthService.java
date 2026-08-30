package com.orgsite.service;

import com.orgsite.dto.*;
import com.orgsite.entity.Organization;
import com.orgsite.entity.User;
import com.orgsite.exception.BadRequestException;
import com.orgsite.repository.OrganizationRepository;
import com.orgsite.repository.UserRepository;
import com.orgsite.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Pattern SLUG_INVALID_CHARS = Pattern.compile("[^a-z0-9-]");

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        Organization.Category category;
        try {
            category = Organization.Category.valueOf(req.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + req.getCategory());
        }

        String slug = generateUniqueSlug(req.getOrganizationName());

        Organization org = Organization.builder()
                .name(req.getOrganizationName())
                .slug(slug)
                .category(category)
                .published(false)
                .build();
        org = organizationRepository.save(org);

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.OWNER)
                .organization(org)
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }

        return buildAuthResponse(user);
    }
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        String token = req.getRefreshToken();
        if (!jwtTokenProvider.isValid(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String email = jwtTokenProvider.getEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Account no longer exists"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgSlug = user.getOrganization() != null ? user.getOrganization().getSlug() : null;

        String access = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name(), orgId);
        String refresh = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name(), orgId);

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationId(orgId)
                .organizationSlug(orgSlug)
                .build();
    }

    private String generateUniqueSlug(String orgName) {
        String base = orgName.toLowerCase().trim()
                .replaceAll("\\s+", "-");
        base = SLUG_INVALID_CHARS.matcher(base).replaceAll("");
        if (base.isBlank()) {
            base = "business";
        }
        String candidate = base;
        int suffix = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
