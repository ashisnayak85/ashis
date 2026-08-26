package com.doctorapp.controller;

import com.doctorapp.dto.AvailabilityResponse;
import com.doctorapp.dto.ClinicRequest;
import com.doctorapp.dto.ClinicSummaryDTO;
import com.doctorapp.dto.DoctorClinicAssociationDTO;
import com.doctorapp.dto.InviteDoctorRequest;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.ClinicAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Clinic-admin-only: create/manage clinics owned by the logged-in clinic admin,
 * and manage which doctors are associated with those clinics.
 */
@RestController
@RequestMapping("/api/clinic-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLINIC_ADMIN')")
public class ClinicAdminController {

    private final ClinicAdminService clinicAdminService;

    @PostMapping("/clinics")
    public ResponseEntity<ClinicSummaryDTO> createClinic(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ClinicRequest req) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.createClinic(clinicAdminId, req));
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<ClinicSummaryDTO>> myClinics(@AuthenticationPrincipal UserPrincipal principal) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.getMyClinics(clinicAdminId));
    }

    @GetMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<List<DoctorClinicAssociationDTO>> clinicDoctors(@AuthenticationPrincipal UserPrincipal principal,
                                                                           @PathVariable Long clinicId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.getClinicDoctors(clinicAdminId, clinicId));
    }

    @GetMapping("/associations")
    public ResponseEntity<List<DoctorClinicAssociationDTO>> allAssociations(@AuthenticationPrincipal UserPrincipal principal) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.getAllAssociations(clinicAdminId));
    }

    @PostMapping("/clinics/{clinicId}/doctors/invite")
    public ResponseEntity<DoctorClinicAssociationDTO> inviteDoctor(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PathVariable Long clinicId,
                                                                    @Valid @RequestBody InviteDoctorRequest req) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.inviteDoctor(clinicAdminId, clinicId, req.getDoctorEmail()));
    }

    @PutMapping("/associations/{associationId}/approve")
    public ResponseEntity<DoctorClinicAssociationDTO> approveRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                                      @PathVariable Long associationId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.respondToJoinRequest(clinicAdminId, associationId, true));
    }

    @PutMapping("/associations/{associationId}/reject")
    public ResponseEntity<DoctorClinicAssociationDTO> rejectRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @PathVariable Long associationId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.respondToJoinRequest(clinicAdminId, associationId, false));
    }

    @DeleteMapping("/associations/{associationId}")
    public ResponseEntity<Void> removeDoctor(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long associationId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        clinicAdminService.removeDoctor(clinicAdminId, associationId);
        return ResponseEntity.noContent().build();
    }

    /** Weekly hours every doctor has set at this clinic, so the owner can see what's live. */
    @GetMapping("/clinics/{clinicId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> clinicAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                                                          @PathVariable Long clinicId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.getClinicAvailability(clinicAdminId, clinicId));
    }

    /** Clinic admin's override: switch a doctor's availability window at this clinic on/off. */
    @PutMapping("/clinics/{clinicId}/availability/{availabilityId}/activate")
    public ResponseEntity<AvailabilityResponse> activateAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @PathVariable Long clinicId,
                                                                       @PathVariable Long availabilityId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.setAvailabilityActive(clinicAdminId, clinicId, availabilityId, true));
    }

    @PutMapping("/clinics/{clinicId}/availability/{availabilityId}/deactivate")
    public ResponseEntity<AvailabilityResponse> deactivateAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @PathVariable Long clinicId,
                                                                        @PathVariable Long availabilityId) {
        Long clinicAdminId = clinicAdminService.getClinicAdminIdForUser(principal.getId());
        return ResponseEntity.ok(clinicAdminService.setAvailabilityActive(clinicAdminId, clinicId, availabilityId, false));
    }
}
