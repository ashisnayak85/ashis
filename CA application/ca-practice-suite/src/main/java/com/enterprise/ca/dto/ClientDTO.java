package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientDTO {
    private Long id;

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 150)
    private String name;

    @NotBlank(message = "Client type is required")
    private String clientType;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Enter a valid 15-character GSTIN")
    private String gstin;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Enter a valid 10-character PAN")
    private String pan;

    @Email(message = "Enter a valid email")
    private String email;

    private String phone;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private Boolean active;

    // Read-only rollups shown on the client list/detail screen
    private java.math.BigDecimal totalIncome;
    private java.math.BigDecimal totalExpense;
    private Long pendingComplianceCount;
}
