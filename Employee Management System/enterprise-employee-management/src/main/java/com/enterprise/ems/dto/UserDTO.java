package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    // Required only when CREATING a user - the admin picks an existing active
    // employee instead of typing a username/email/password by hand.
    @NotNull(message = "Employee is required")
    private Long employeeId;

    // Output-only fields: derived from the linked Employee, never taken from client input.
    private String username;
    private String email;

    @NotBlank(message = "Role is required")
    private String roleName;

    private Boolean enabled;
}
