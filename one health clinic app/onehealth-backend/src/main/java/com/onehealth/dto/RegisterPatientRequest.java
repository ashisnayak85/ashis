package com.onehealth.dto;

import com.onehealth.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterPatientRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    // Indian mobile number: 10 digits, starting 6-9. Adjust the pattern if the
    // clinic-chain operates outside India.
    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String phone;

    // Which clinic-chain the patient is signing up under, e.g. "one-health".
    @NotBlank
    private String organizationSlug;

    private LocalDate dob;
    private Gender gender;

    // Added for location-based business analytics (see Patient entity javadoc).
    private String city;
    private String pincode;
}
