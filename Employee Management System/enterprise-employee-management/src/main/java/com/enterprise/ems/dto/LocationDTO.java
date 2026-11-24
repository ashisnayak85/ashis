package com.enterprise.ems.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDTO {

    private Long id;

    @NotBlank(message = "Location name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Location code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric")
    private String code;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 10)
    private String pincode;

    @NotBlank(message = "Office contact number is required")
    @Pattern(
        regexp = "^\\+?[0-9][0-9\\-\\s]{6,18}[0-9]$",
        message = "Enter a valid contact number (digits only, optional leading +, 7-20 characters)"
    )
    private String officeContact;

    // Not user-editable through the form today - defaults on the entity side.
    // Exposed here now so a future working-hours/holiday feature can read/set
    // it without another DTO change.
    private String timezone;

    private Boolean active;

    private Long employeeCount;
}
