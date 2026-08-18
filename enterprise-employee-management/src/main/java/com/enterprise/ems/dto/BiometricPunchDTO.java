package com.enterprise.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

/*
 * PURPOSE: Payload pushed by a biometric attendance machine. Devices identify
 * an employee by their badge/employee code (not a login), and report a single
 * IN or OUT punch with a timestamp - the server figures out which attendance
 * row that belongs to and whether it's a check-in or check-out.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricPunchDTO {

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotNull(message = "Punch timestamp is required")
    private LocalDateTime timestamp;

    @NotBlank(message = "Punch type is required")
    @Pattern(regexp = "IN|OUT", message = "Punch type must be IN or OUT")
    private String punchType;
}
