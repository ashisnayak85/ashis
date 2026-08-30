package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.ClinicService;
import com.onehealth.service.DashboardService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.SpecializationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * OWNER-only: manages every branch/doctor/front-desk login across the whole
 * organization, and the cross-branch analytics dashboard. Every method scopes
 * to CurrentUser.organizationId() - the owner never sees another org's data,
 * even in a future multi-tenant deployment with several clinic-chain customers.
 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

    private final ClinicService clinicService;
    private final DoctorService doctorService;
    private final DashboardService dashboardService;
    private final SpecializationService specializationService;

    // --- Clinics (branches) ---

    @PostMapping("/clinics")
    public ResponseEntity<ClinicDTO> createClinic(@Valid @RequestBody ClinicRequest req) {
        return ResponseEntity.ok(clinicService.createClinic(CurrentUser.organizationId(), req));
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<ClinicDTO>> listClinics() {
        return ResponseEntity.ok(clinicService.listClinics(CurrentUser.organizationId()));
    }

    @PutMapping("/clinics/{id}/status")
    public ResponseEntity<ClinicDTO> setClinicStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(clinicService.setClinicActive(CurrentUser.organizationId(), id, active));
    }

    @PostMapping("/clinic-admins")
    public ResponseEntity<Void> registerClinicAdmin(@Valid @RequestBody RegisterClinicAdminRequest req) {
        clinicService.registerClinicAdmin(CurrentUser.organizationId(), req);
        return ResponseEntity.ok().build();
    }

    // --- Doctors ---

    @PostMapping("/doctors")
    public ResponseEntity<DoctorDTO> registerDoctor(@Valid @RequestBody RegisterDoctorRequest req) {
        return ResponseEntity.ok(doctorService.registerDoctor(CurrentUser.organizationId(), req));
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDTO>> listDoctors() {
        return ResponseEntity.ok(doctorService.listDoctors(CurrentUser.organizationId()));
    }

    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<DoctorDTO> setDoctorStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(doctorService.setDoctorActive(CurrentUser.organizationId(), id, active));
    }

    @PostMapping("/doctors/assign")
    public ResponseEntity<Void> assignDoctor(@Valid @RequestBody AssignDoctorRequest req) {
        doctorService.assignToClinic(CurrentUser.organizationId(), req.getDoctorId(), req.getClinicId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/doctors/unassign")
    public ResponseEntity<Void> unassignDoctor(@Valid @RequestBody AssignDoctorRequest req) {
        doctorService.unassignFromClinic(CurrentUser.organizationId(), req.getDoctorId(), req.getClinicId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/doctors/{id}/specializations")
    public ResponseEntity<DoctorDTO> updateDoctorSpecializations(@PathVariable Long id,
                                                                  @RequestBody UpdateDoctorSpecializationsRequest req) {
        return ResponseEntity.ok(doctorService.updateSpecializations(
                CurrentUser.organizationId(), id, req.getSpecializationIds()));
    }

    // --- Specializations (master list) ---

    @PostMapping("/specializations")
    public ResponseEntity<SpecializationDTO> createSpecialization(@Valid @RequestBody SpecializationRequest req) {
        return ResponseEntity.ok(specializationService.create(CurrentUser.organizationId(), req));
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<SpecializationDTO>> listSpecializations() {
        return ResponseEntity.ok(specializationService.list(CurrentUser.organizationId()));
    }

    @PutMapping("/specializations/{id}")
    public ResponseEntity<SpecializationDTO> renameSpecialization(@PathVariable Long id, @Valid @RequestBody SpecializationRequest req) {
        return ResponseEntity.ok(specializationService.rename(CurrentUser.organizationId(), id, req.getName()));
    }

    @PutMapping("/specializations/{id}/status")
    public ResponseEntity<SpecializationDTO> setSpecializationStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(specializationService.setActive(CurrentUser.organizationId(), id, active));
    }

    @DeleteMapping("/specializations/{id}")
    public ResponseEntity<Void> deleteSpecialization(@PathVariable Long id) {
        specializationService.delete(CurrentUser.organizationId(), id);
        return ResponseEntity.noContent().build();
    }

    // --- Dashboard ---

    /**
     * Defaults from/to to today when not supplied - "by default it will load
     * current date data". Pass clinicId to drill into one branch; omit for the
     * all-branches, location-wise comparison view.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> dashboard(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long clinicId) {
        DashboardFilter filter = new DashboardFilter();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setClinicId(clinicId);
        return ResponseEntity.ok(dashboardService.getStats(CurrentUser.organizationId(), filter));
    }
}
