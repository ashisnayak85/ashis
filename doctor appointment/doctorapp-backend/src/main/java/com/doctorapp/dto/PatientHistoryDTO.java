package com.doctorapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** A patient the doctor has seen, grouped with every appointment they've had with this doctor. */
@Getter
@Builder
@AllArgsConstructor
public class PatientHistoryDTO {
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private int totalVisits;
    private List<DoctorAppointmentDTO> appointments;
}
