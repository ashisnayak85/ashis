package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * PURPOSE: Data Transfer Object for Employee - decouples API/UI from Entity
 * WHY DTO: Never expose JPA entities directly (lazy loading, security risks)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "Employee code is required")
    @Size(max = 20, message = "Employee code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Employee code must be uppercase alphanumeric")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    private LocalDate dateOfBirth;

    @NotNull(message = "Date of joining is required")
    private LocalDate dateOfJoining;

    @Min(value = 0, message = "Salary cannot be negative")
    private BigDecimal salary;

    private String designation;

    private String profilePhoto;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;

    private Boolean active;
}
