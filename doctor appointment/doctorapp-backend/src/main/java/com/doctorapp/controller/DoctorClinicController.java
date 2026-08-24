package com.doctorapp.controller;

import com.doctorapp.dto.ClinicSummaryDTO;
import com.doctorapp.dto.DoctorClinicAssociationDTO;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor-only: browsing clinics to join, requesting to join one, and responding to
 * invites sent by clinic admins. See DoctorClinicAssociation for the approval model.
 */
@RestController
@RequestMapping("/api/doctor/clinics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorClinicController {

    private final DoctorService doctorService;

    @GetMapping("/browse")
    public ResponseEntity<List<ClinicSummaryDTO>> browse(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(doctorService.browseClinics(city));
    }

    @GetMapping
    public ResponseEntity<List<DoctorClinicAssociationDTO>> myAssociations(@AuthenticationPrincipal UserPrincipal principal) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(doctorService.getMyAssociations(doctorId));
    }

    @PostMapping("/{clinicId}/join-request")
    public ResponseEntity<DoctorClinicAssociationDTO> requestJoin(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @PathVariable Long clinicId) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(doctorService.requestJoinClinic(doctorId, clinicId));
    }

    @PutMapping("/associations/{associationId}/approve")
    public ResponseEntity<DoctorClinicAssociationDTO> approveInvite(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @PathVariable Long associationId) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(doctorService.respondToInvite(doctorId, associationId, true));
    }

    @PutMapping("/associations/{associationId}/reject")
    public ResponseEntity<DoctorClinicAssociationDTO> rejectInvite(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PathVariable Long associationId) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(doctorService.respondToInvite(doctorId, associationId, false));
    }

    @DeleteMapping("/associations/{associationId}")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long associationId) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        doctorService.leaveClinic(doctorId, associationId);
        return ResponseEntity.noContent().build();
    }
}
