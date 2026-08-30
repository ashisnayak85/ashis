package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.Patient;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.ClinicRepository;
import com.onehealth.repository.PatientRepository;
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

/** PATIENT-only: browse this organization's branches/doctors, book/cancel own appointments. */
@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final ClinicRepository clinicRepository;
    private final DoctorService doctorService;
    private final PatientRepository patientRepository;
    private final SlotService slotService;
    private final AppointmentService appointmentService;

    @GetMapping("/clinics")
    public ResponseEntity<List<Clinic>> clinics() {
        return ResponseEntity.ok(clinicRepository.findByOrganizationIdAndActiveTrue(CurrentUser.organizationId()));
    }

    @GetMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<List<DoctorDTO>> doctorsAtClinic(@PathVariable Long clinicId) {
        return ResponseEntity.ok(doctorService.getActiveDoctorsAtClinic(clinicId));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> getSlots(@RequestParam Long doctorId, @RequestParam Long clinicId,
                                                   @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.getOrGenerateSlots(doctorId, clinicId, date));
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDTO> book(@Valid @RequestBody BookAppointmentRequest req) {
        return ResponseEntity.ok(appointmentService.bookOnline(
                CurrentUser.organizationId(), CurrentUser.userId(), req.getSlotId()));
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(
                CurrentUser.organizationId(), CurrentUser.userId(), id, true));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDTO>> myAppointments() {
        Patient patient = patientRepository.findByUserId(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return ResponseEntity.ok(appointmentService.myAppointments(patient.getId()));
    }
}
