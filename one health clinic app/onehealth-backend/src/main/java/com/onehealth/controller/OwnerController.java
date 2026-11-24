package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.ClinicService;
import com.onehealth.service.DashboardService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.LocationAnalyticsService;
import com.onehealth.service.MedicineService;
import com.onehealth.service.SlotService;
import com.onehealth.service.SpecializationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final SlotService slotService;
    private final MedicineService medicineService;
    private final LocationAnalyticsService locationAnalyticsService;

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

    // --- Doctor time periods (availability) - owner has the same on/off access
    // as a clinic authority, but across any branch in the org rather than just
    // one. Creating/removing a time period stays doctor-only; this is
    // specifically the "turn a time period on/off" power. ---

    /** Every time period for every doctor, across the whole org (or one branch via ?clinicId=). */
    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> organizationAvailability(
            @RequestParam(required = false) Long clinicId) {
        return ResponseEntity.ok(slotService.getOrganizationAvailability(CurrentUser.organizationId(), clinicId));
    }

    @PutMapping("/availability/{id}/status")
    public ResponseEntity<AvailabilityResponse> setAvailabilityActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(slotService.setAvailabilityActiveForOwner(
                CurrentUser.organizationId(), id, active, CurrentUser.userId(), CurrentUser.role()));
    }

    /** Who created/removed/turned on/off which time period, and when - across the whole org (or one branch via ?clinicId=). */
    @GetMapping("/availability-history")
    public ResponseEntity<List<AvailabilityAuditLogDTO>> availabilityHistory(
            @RequestParam(required = false) Long clinicId) {
        return ResponseEntity.ok(slotService.getHistoryForOrganization(CurrentUser.organizationId(), clinicId));
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
        return ResponseEntity.ok(dashboardService.getStats(CurrentUser.organizationId(), filter, true));
    }

    // --- Medicine catalog (org-wide) ---

    @PostMapping("/medicines")
    public ResponseEntity<MedicineDTO> createMedicine(@Valid @RequestBody MedicineRequest req) {
        return ResponseEntity.ok(medicineService.createMedicine(CurrentUser.organizationId(), req));
    }

    @GetMapping("/medicines")
    public ResponseEntity<List<MedicineDTO>> listMedicines() {
        return ResponseEntity.ok(medicineService.listMedicines(CurrentUser.organizationId()));
    }

    @PutMapping("/medicines/{id}/status")
    public ResponseEntity<MedicineDTO> setMedicineStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(medicineService.setMedicineActive(CurrentUser.organizationId(), id, active));
    }

    // --- Stock dispatch: owner sends stock to a branch; clinic admins only view + set margin.
    // See StockBatch's class javadoc for why dispatch is centralized here. ---

    @PostMapping("/stock/dispatch")
    public ResponseEntity<ClinicMedicineStockDTO> dispatchStock(@Valid @RequestBody DispatchStockRequest req) {
        return ResponseEntity.ok(medicineService.dispatchStock(
                CurrentUser.organizationId(), CurrentUser.userId(), req));
    }

    /** A branch's current stock, for the owner to check before/after dispatching more. */
    @GetMapping("/stock/{clinicId}")
    public ResponseEntity<List<ClinicMedicineStockDTO>> clinicStock(@PathVariable Long clinicId) {
        return ResponseEntity.ok(medicineService.getClinicStock(clinicId));
    }

    /** Dispatched vs. sold vs. remaining, every branch/medicine - the shrinkage-detection report. */
    @GetMapping("/stock/reconciliation")
    public ResponseEntity<List<StockReconciliationDTO>> stockReconciliation() {
        Map<Long, String> clinicNames = clinicService.listClinics(CurrentUser.organizationId()).stream()
                .collect(Collectors.toMap(ClinicDTO::getId, ClinicDTO::getClinicName));
        return ResponseEntity.ok(medicineService.getReconciliation(CurrentUser.organizationId(), clinicNames));
    }

    /**
     * OWNER-ONLY: set the chargeable margin for one medicine at one branch.
     * Clinic admins cannot set their own margin - see ClinicMedicineStock's
     * class javadoc for why pricing authority is centralized here.
     */
    @PutMapping("/clinics/{clinicId}/stock/{medicineId}/margin")
    public ResponseEntity<ClinicMedicineStockDTO> updateMargin(
            @PathVariable Long clinicId, @PathVariable Long medicineId,
            @Valid @RequestBody UpdateMarginRequest req) {
        return ResponseEntity.ok(medicineService.updateMargin(CurrentUser.organizationId(), clinicId, medicineId, req));
    }

    // --- Prescriptions/bills (owner sees full cost/profit detail; see
    // ClinicAdminController for the doctor/admin-facing, price-limited views) ---

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<PrescriptionDTO> prescriptionFullDetail(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getPrescriptionFullDetail(CurrentUser.organizationId(), id));
    }

    // --- Expansion analytics: which pincodes send us the most patients from
    // furthest away - see LocationAnalyticsService for the scoring logic. ---

    @GetMapping("/expansion-opportunities")
    public ResponseEntity<List<LocationOpportunityDTO>> expansionOpportunities() {
        return ResponseEntity.ok(locationAnalyticsService.getExpansionOpportunities(CurrentUser.organizationId()));
    }
}
