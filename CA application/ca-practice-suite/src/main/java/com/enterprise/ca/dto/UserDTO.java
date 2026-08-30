package com.enterprise.ca.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    // Only required on create; ignored on update (password change is separate).
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private Boolean enabled;

    @NotEmpty(message = "At least one role is required")
    private List<String> roles;
}
