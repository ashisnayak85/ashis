package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.entity.Doctor;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.DoctorRepository;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.AppointmentService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** DOCTOR-only: manage own weekly availability (across their assigned branches) and view own schedule. */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final SlotService slotService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;

    @GetMapping("/my-clinics")
    public ResponseEntity<List<ClinicDTO>> myClinics() {
        return ResponseEntity.ok(doctorService.getMyAssignedClinics(myDoctorId()));
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> addAvailability(@Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(slotService.addAvailability(myDoctorId(), req));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> myAvailability() {
        return ResponseEntity.ok(slotService.getMyAvailability(myDoctorId()));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<SlotResyncResultDTO> deleteAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(slotService.deleteAvailability(myDoctorId(), id));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDTO>> myAppointments(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.doctorAppointments(myDoctorId(), date));
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<AppointmentDTO> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        return ResponseEntity.ok(appointmentService.updateStatus(CurrentUser.organizationId(), id, req));
    }

    private Long myDoctorId() {
        Doctor doctor = doctorRepository.findByUserId(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return doctor.getId();
    }
}
