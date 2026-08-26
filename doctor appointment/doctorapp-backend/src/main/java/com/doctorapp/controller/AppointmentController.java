package com.doctorapp.controller;

import com.doctorapp.dto.AppointmentDTO;
import com.doctorapp.dto.BookAppointmentRequest;
import com.doctorapp.dto.RatingDTO;
import com.doctorapp.dto.RatingRequest;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.AppointmentService;
import com.doctorapp.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Patient-only: book, view, and cancel appointments. */
@RestController
@RequestMapping("/api/patient/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<AppointmentDTO> book(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody BookAppointmentRequest req) {
        return ResponseEntity.ok(appointmentService.bookSlot(principal.getId(), req.getSlotId()));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> myAppointments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.myAppointments(principal.getId()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(principal.getId(), id));
    }

    /** Rate & review a completed appointment. One rating per appointment - see RatingService. */
    @PostMapping("/{id}/rating")
    public ResponseEntity<RatingDTO> rate(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @Valid @RequestBody RatingRequest req) {
        return ResponseEntity.ok(
                ratingService.submitRating(principal.getId(), id, req.getRating(), req.getReviewText()));
    }
}
