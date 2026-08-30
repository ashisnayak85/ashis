package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntryDTO {
    private Long id;

    private Long clientId;
    private String clientName;

    @NotNull(message = "Ledger account is required")
    private Long accountId;
    private String accountName;
    private String accountType;

    @NotBlank(message = "Entry type is required")
    private String entryType;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;

    @Size(max = 500)
    private String description;

    private String referenceNumber;
    private Boolean reconciled;
    private String createdBy;
}
