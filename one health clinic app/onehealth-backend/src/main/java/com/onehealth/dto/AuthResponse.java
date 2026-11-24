package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Long organizationId;
    private String organizationName;
    private Long userId;
    private String name;
}
