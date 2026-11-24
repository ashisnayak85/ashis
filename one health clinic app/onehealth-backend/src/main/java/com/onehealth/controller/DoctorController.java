package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.entity.Appointment;
import com.onehealth.entity.Doctor;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.AppointmentRepository;
import com.onehealth.repository.DoctorRepository;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.AppointmentService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.MedicineService;
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
    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final MedicineService medicineService;

    @GetMapping("/my-clinics")
    public ResponseEntity<List<ClinicDTO>> myClinics() {
        return ResponseEntity.ok(doctorService.getMyAssignedClinics(myDoctorId()));
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> addAvailability(@Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(slotService.addAvailability(myDoctorId(), req, CurrentUser.userId(), CurrentUser.role()));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> myAvailability() {
        return ResponseEntity.ok(slotService.getMyAvailability(myDoctorId()));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<SlotResyncResultDTO> deleteAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(slotService.deleteAvailability(myDoctorId(), id, CurrentUser.userId(), CurrentUser.role()));
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

    // --- Prescribing (clinical only - no pricing, no stock deduction; the
    // clinic admin bills it separately, see ClinicAdminController) ---

    /** This appointment's clinic's current stock - selling price only, no cost/margin (see ClinicStockViewDTO). */
    @GetMapping("/appointments/{id}/available-medicines")
    public ResponseEntity<List<ClinicStockViewDTO>> availableMedicines(@PathVariable Long id) {
        Long clinicId = appointmentClinicId(id);
        return ResponseEntity.ok(medicineService.getClinicStockPublicView(clinicId));
    }

    @PostMapping("/appointments/{id}/prescriptions")
    public ResponseEntity<PrescriptionSummaryDTO> prescribe(@PathVariable Long id,
                                                       @Valid @RequestBody CreatePrescriptionRequest req) {
        return ResponseEntity.ok(medicineService.createPrescription(
                CurrentUser.organizationId(), myDoctorId(), id, req));
    }

    @GetMapping("/appointments/{id}/prescriptions")
    public ResponseEntity<PrescriptionSummaryDTO> getPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getPrescriptionForAppointment(id));
    }

    private Long appointmentClinicId(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getDoctor().getId().equals(myDoctorId())) {
            throw new AccessDeniedBusinessException("You can only view your own appointments.");
        }
        return appointment.getClinic().getId();
    }

    private Long myDoctorId() {
        Doctor doctor = doctorRepository.findByUserId(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return doctor.getId();
    }
}
