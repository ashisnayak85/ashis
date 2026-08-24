package com.doctorapp.controller;

import com.doctorapp.dto.AvailabilityRequest;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.DoctorService;
import com.doctorapp.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Doctor-only: define the weekly recurring working-hours template. */
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
}
