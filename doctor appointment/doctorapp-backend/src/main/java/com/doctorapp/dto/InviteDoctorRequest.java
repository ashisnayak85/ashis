package com.doctorapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteDoctorRequest {
    @NotBlank @Email
    private String doctorEmail;
}
