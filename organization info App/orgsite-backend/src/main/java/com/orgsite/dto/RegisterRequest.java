package com.orgsite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Self-service signup: creates BOTH a new Organization and its first OWNER user in one call. */
@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Organization name is required")
    private String organizationName;

    @NotBlank(message = "Category is required")
    private String category; // matches Organization.Category enum name
}
