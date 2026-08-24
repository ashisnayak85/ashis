package com.doctorapp.controller;

import com.doctorapp.dto.AdminAppointmentDTO;
import com.doctorapp.dto.ClinicSummaryDTO;
import com.doctorapp.dto.DashboardStatsDTO;
import com.doctorapp.dto.DoctorSummaryDTO;
import com.doctorapp.dto.PatientSummaryDTO;
import com.doctorapp.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only: platform oversight - doctor verification, patient/appointment visibility, dashboard stats. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> dashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorSummaryDTO>> doctors(@RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(adminService.listDoctors(verified));
    }

    @PutMapping("/doctors/{id}/verify")
    public ResponseEntity<DoctorSummaryDTO> verifyDoctor(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.verifyDoctor(id));
    }

    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<DoctorSummaryDTO> setDoctorStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setDoctorActive(id, active));
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<ClinicSummaryDTO>> clinics(@RequestParam(required = false) Boolean verified) {
        return ResponseEntity.ok(adminService.listClinics(verified));
    }

    @PutMapping("/clinics/{id}/verify")
    public ResponseEntity<ClinicSummaryDTO> verifyClinic(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.verifyClinic(id));
    }

    @PutMapping("/clinics/{id}/status")
    public ResponseEntity<ClinicSummaryDTO> setClinicStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(adminService.setClinicActive(id, active));
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientSummaryDTO>> patients() {
        return ResponseEntity.ok(adminService.listPatients());
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AdminAppointmentDTO>> appointments() {
        return ResponseEntity.ok(adminService.listAppointments());
    }
}
