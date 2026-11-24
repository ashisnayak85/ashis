package com.onehealth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** SUPER_ADMIN-only: onboard a new clinic-chain customer + their first Owner login. */
@Data
public class CreateOrganizationRequest {
    @NotBlank
    private String organizationName;

    @NotBlank
    private String organizationSlug;

    private String supportPhone;

    @NotBlank @Email
    private String ownerEmail;

    @NotBlank
    private String ownerPassword;

    @NotBlank
    private String ownerName;
}
