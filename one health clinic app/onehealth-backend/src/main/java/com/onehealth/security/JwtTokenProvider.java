package com.onehealth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    // organizationId is embedded as a claim so every downstream request is
    // self-scoped to a tenant without an extra DB lookup, and can't be spoofed
    // by the client (it's baked into the signed token at login time).
    public String generateAccessToken(String email, String role, Long organizationId) {
        return buildToken(email, role, organizationId, accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(String email, String role, Long organizationId) {
        return buildToken(email, role, organizationId, refreshTokenExpiryMs, "refresh");
    }

    private String buildToken(String email, String role, Long organizationId, long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
        var builder = Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry);
        if (organizationId != null) {
            builder.claim("orgId", organizationId);
        }
        return builder.signWith(key).compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type"));
    }
}
