package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class PatientSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dob;
}
