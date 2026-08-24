package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DoctorClinicAssociationDTO {
    private Long id;

    private Long clinicId;
    private String clinicName;
    private String clinicAddress;

    private Long doctorId;
    private String doctorName;
    private String doctorQualification;

    private String initiatedBy; // DOCTOR or CLINIC
    private String status;      // PENDING, APPROVED, REJECTED, REMOVED
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
