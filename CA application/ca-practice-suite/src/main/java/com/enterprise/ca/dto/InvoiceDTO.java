package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;

    @NotNull(message = "Client is required")
    private Long clientId;
    private String clientName;

    @NotBlank(message = "Invoice type is required")
    private String invoiceType;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    private String description;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be greater than zero")
    private BigDecimal subtotal;

    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String status;
}
