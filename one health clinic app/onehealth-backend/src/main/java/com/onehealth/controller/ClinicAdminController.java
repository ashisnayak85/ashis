package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.entity.ClinicAdmin;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.ClinicAdminRepository;
import com.onehealth.repository.PatientRepository;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.AppointmentService;
import com.onehealth.service.DashboardService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CLINIC_ADMIN-only: the "clinic authority" for exactly one branch. Handles
 * walk-in booking, that branch's schedule/appointments, and a scoped view of
 * the analytics dashboard (their own branch only - clinicId is always forced
 * server-side to their own, never taken from the request).
 */
@RestController
@RequestMapping("/api/clinic-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLINIC_ADMIN')")
public class ClinicAdminController {

    private final ClinicAdminRepository clinicAdminRepository;
    private final PatientRepository patientRepository;
    private final AppointmentService appointmentService;
    private final SlotService slotService;
    private final DashboardService dashboardService;
    private final DoctorService doctorService;

    /**
     * Doctors assigned to this branch, so the walk-in booking screen can show a
     * name dropdown instead of asking front-desk staff to somehow know a
     * doctor's numeric database ID.
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDTO>> doctorsAtMyClinic() {
        return ResponseEntity.ok(doctorService.getActiveDoctorsAtClinic(myClinicId()));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> getSlots(@RequestParam Long doctorId,
                                                   @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        Long clinicId = myClinicId();
        return ResponseEntity.ok(slotService.getOrGenerateSlots(doctorId, clinicId, date));
    }

    /** Books a walk-in patient (registered app user or not) against an open slot at this branch. */
    @PostMapping("/appointments/walk-in")
    public ResponseEntity<AppointmentDTO> bookWalkIn(@Valid @RequestBody WalkInBookingRequest req) {
        return ResponseEntity.ok(appointmentService.bookWalkIn(
                CurrentUser.organizationId(), CurrentUser.userId(), req));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDTO>> appointments(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.clinicAppointments(myClinicId(), date));
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<AppointmentDTO> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        return ResponseEntity.ok(appointmentService.updateStatus(CurrentUser.organizationId(), id, req));
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(
                CurrentUser.organizationId(), CurrentUser.userId(), id, false));
    }

    @PutMapping("/availability/{id}/status")
    public ResponseEntity<AvailabilityResponse> setAvailabilityActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(slotService.setAvailabilityActive(myClinicId(), id, active));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> clinicAvailability() {
        return ResponseEntity.ok(slotService.getClinicAvailability(myClinicId()));
    }

    /** This branch's own dashboard - clinicId is forced to their branch regardless of query params. */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> dashboard(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardFilter filter = new DashboardFilter();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setClinicId(myClinicId());
        return ResponseEntity.ok(dashboardService.getStats(CurrentUser.organizationId(), filter));
    }

    private Long myClinicId() {
        ClinicAdmin admin = clinicAdminRepository.findByUserId(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic admin profile not found"));
        return admin.getClinicId();
    }
}
