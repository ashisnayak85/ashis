package com.doctorapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterClinicAdminRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String phone;

    // A clinic admin registers their account first, then creates clinic(s)
    // via a separate endpoint - same "lean registration" pattern as doctors.
}
