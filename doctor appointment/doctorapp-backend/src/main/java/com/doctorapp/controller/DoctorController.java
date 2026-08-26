package com.doctorapp.controller;

import com.doctorapp.dto.DoctorProfileDTO;
import com.doctorapp.dto.DoctorRatingSummaryDTO;
import com.doctorapp.dto.NearbyDoctorResult;
import com.doctorapp.dto.SlotDTO;
import com.doctorapp.service.DoctorService;
import com.doctorapp.service.RatingService;
import com.doctorapp.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public browsing endpoints (nearby search, profile, slots, ratings) live here
 * unauthenticated - see SecurityConfig. Booking itself requires a logged-in
 * patient (AppointmentController), and so does submitting a rating.
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final SlotService slotService;
    private final RatingService ratingService;

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyDoctorResult>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String specialization) {
        return ResponseEntity.ok(doctorService.findNearby(lat, lng, radiusKm, specialization));
    }

    @GetMapping("/{doctorId}/profile")
    public ResponseEntity<DoctorProfileDTO> profile(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getProfile(doctorId));
    }

    @GetMapping("/{doctorId}/slots")
    public ResponseEntity<List<SlotDTO>> slots(
            @PathVariable Long doctorId,
            @RequestParam Long clinicId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.getOrGenerateSlots(doctorId, clinicId, date));
    }

    /** Average rating, star distribution, and one page of reviews for this doctor. */
    @GetMapping("/{doctorId}/ratings")
    public ResponseEntity<DoctorRatingSummaryDTO> ratings(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ratingService.getDoctorRatingSummary(doctorId, page, size));
    }
}
