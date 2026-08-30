package com.onehealth.dto;

import com.onehealth.entity.Gender;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Owner-only view - includes salary, so never reuse this shape on any non-owner endpoint. */
@Getter
@Builder
public class EmployeeProfileDTO {
    private Long userId;
    private String name;
    private String role; // CLINIC_ADMIN or DOCTOR
    private List<String> clinicNames;

    private Gender gender;
    private LocalDate dob;
    private LocalDate dateOfJoining;
    private String permanentAddress;
    private String currentAddress;

    private BigDecimal currentSalary;
    private LocalDate currentSalaryEffectiveFrom;
    private List<SalaryRecordDTO> salaryHistory;
}
