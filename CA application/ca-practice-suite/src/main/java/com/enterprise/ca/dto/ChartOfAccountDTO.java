package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccountDTO {
    private Long id;

    @NotBlank(message = "Account name is required")
    private String name;

    @NotBlank(message = "Account code is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must be uppercase alphanumeric")
    private String code;

    @NotBlank(message = "Account type is required")
    private String accountType;

    private Boolean active;
}
