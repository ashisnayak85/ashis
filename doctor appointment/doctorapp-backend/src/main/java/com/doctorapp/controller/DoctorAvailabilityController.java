package com.doctorapp.controller;

import com.doctorapp.dto.AvailabilityRequest;
import com.doctorapp.dto.AvailabilityResponse;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.DoctorService;
import com.doctorapp.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Doctor-only: define and review the weekly recurring working-hours template. */
@RestController
@RequestMapping("/api/doctor/availability")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAvailabilityController {

    private final SlotService slotService;
    private final DoctorService doctorService;

    @PostMapping
    public ResponseEntity<?> add(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody AvailabilityRequest req) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(slotService.addAvailability(doctorId, req));
    }

    /** The logged-in doctor's own weekly hours, across every clinic they work at. */
    @GetMapping
    public ResponseEntity<List<AvailabilityResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(slotService.getMyAvailability(doctorId));
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long availabilityId) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        slotService.deleteAvailability(doctorId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
