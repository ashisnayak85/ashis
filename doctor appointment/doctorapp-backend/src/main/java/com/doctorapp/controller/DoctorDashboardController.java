package com.doctorapp.controller;

import com.doctorapp.dto.DoctorAppointmentDTO;
import com.doctorapp.dto.DoctorProfileDTO;
import com.doctorapp.dto.PatientHistoryDTO;
import com.doctorapp.dto.UpdateAppointmentStatusRequest;
import com.doctorapp.security.UserPrincipal;
import com.doctorapp.service.AppointmentService;
import com.doctorapp.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Doctor-only: the logged-in doctor's own profile, appointment queue, and patient history. */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorDashboardController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @GetMapping("/me/profile")
    public ResponseEntity<DoctorProfileDTO> myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Long doctorId = doctorService.getDoctorIdForUser(principal.getId());
        return ResponseEntity.ok(doctorService.getProfile(doctorId));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<DoctorAppointmentDTO>> myAppointments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.doctorAppointments(principal.getId()));
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<DoctorAppointmentDTO> updateStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        return ResponseEntity.ok(appointmentService.updateStatusByDoctor(principal.getId(), id, req.getStatus()));
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientHistoryDTO>> myPatients(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.doctorPatients(principal.getId()));
    }
}
