package com.onehealth.controller;

import com.onehealth.dto.*;
import com.onehealth.entity.ClinicAdmin;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.ClinicAdminRepository;
import com.onehealth.repository.PatientRepository;
import com.onehealth.security.CurrentUser;
import com.onehealth.service.AppointmentService;
import com.onehealth.service.DashboardService;
import com.onehealth.service.DoctorService;
import com.onehealth.service.MedicineService;
import com.onehealth.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CLINIC_ADMIN-only: the "clinic authority" for exactly one branch. Handles
 * walk-in booking, that branch's schedule/appointments, and a scoped view of
 * the analytics dashboard (their own branch only - clinicId is always forced
 * server-side to their own, never taken from the request).
 */
@RestController
@RequestMapping("/api/clinic-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLINIC_ADMIN')")
public class ClinicAdminController {

    private final ClinicAdminRepository clinicAdminRepository;
    private final PatientRepository patientRepository;
    private final AppointmentService appointmentService;
    private final SlotService slotService;
    private final DashboardService dashboardService;
    private final DoctorService doctorService;
    private final MedicineService medicineService;

    /**
     * Doctors assigned to this branch, so the walk-in booking screen can show a
     * name dropdown instead of asking front-desk staff to somehow know a
     * doctor's numeric database ID.
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDTO>> doctorsAtMyClinic() {
        return ResponseEntity.ok(doctorService.getActiveDoctorsAtClinic(myClinicId()));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> getSlots(@RequestParam Long doctorId,
                                                   @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        Long clinicId = myClinicId();
        return ResponseEntity.ok(slotService.getOrGenerateSlots(doctorId, clinicId, date));
    }

    /** Books a walk-in patient (registered app user or not) against an open slot at this branch. */
    @PostMapping("/appointments/walk-in")
    public ResponseEntity<AppointmentDTO> bookWalkIn(@Valid @RequestBody WalkInBookingRequest req) {
        return ResponseEntity.ok(appointmentService.bookWalkIn(
                CurrentUser.organizationId(), CurrentUser.userId(), req));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDTO>> appointments(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.clinicAppointments(myClinicId(), date));
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<AppointmentDTO> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        return ResponseEntity.ok(appointmentService.updateStatus(CurrentUser.organizationId(), id, req));
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(
                CurrentUser.organizationId(), CurrentUser.userId(), id, false));
    }

    @PutMapping("/availability/{id}/status")
    public ResponseEntity<AvailabilityResponse> setAvailabilityActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(slotService.setAvailabilityActiveForClinicAdmin(
                myClinicId(), id, active, CurrentUser.userId(), CurrentUser.role()));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> clinicAvailability() {
        return ResponseEntity.ok(slotService.getClinicAvailability(myClinicId()));
    }

    /** Who turned which time period on/off (or created/removed one) at this branch, and when. */
    @GetMapping("/availability-history")
    public ResponseEntity<List<AvailabilityAuditLogDTO>> availabilityHistory() {
        return ResponseEntity.ok(slotService.getHistoryForOrganization(CurrentUser.organizationId(), myClinicId()));
    }

    /** This branch's own dashboard - clinicId is forced to their branch regardless of query params. */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> dashboard(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardFilter filter = new DashboardFilter();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setClinicId(myClinicId());
        return ResponseEntity.ok(dashboardService.getStats(CurrentUser.organizationId(), filter, false));
    }

    // --- Medicine stock (this branch only) ---
    // Note: clinic admins can VIEW stock (selling price only, never cost) and
    // set the low-stock reorder threshold, but cannot set margin/pricing (see
    // OwnerController.updateMargin) or add stock themselves - see StockBatch's
    // class javadoc for why dispatching stock is an owner (HQ) action.

    /** Org-wide catalog, read-only - lets this branch see what medicines exist even though only the owner dispatches them. */
    @GetMapping("/medicines")
    public ResponseEntity<List<MedicineDTO>> medicineCatalog() {
        return ResponseEntity.ok(medicineService.listMedicines(CurrentUser.organizationId()).stream()
                .filter(MedicineDTO::isActive).toList());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<ClinicStockViewDTO>> myStock() {
        return ResponseEntity.ok(medicineService.getClinicStockPublicView(myClinicId()));
    }

    @PutMapping("/stock/{medicineId}/reorder-threshold")
    public ResponseEntity<ClinicStockViewDTO> updateReorderThreshold(
            @PathVariable Long medicineId, @Valid @RequestBody UpdateReorderThresholdRequest req) {
        return ResponseEntity.ok(medicineService.updateReorderThreshold(myClinicId(), medicineId, req));
    }

    // --- Billing: the doctor prescribes (see DoctorController), the clinic
    // admin bills it here - this is the step that actually charges the
    // patient and deducts stock. See Prescription.PrescriptionStatus javadoc. ---

    /** Prescriptions written at this branch that haven't been billed yet - the billing queue. */
    @GetMapping("/prescriptions/pending")
    public ResponseEntity<List<PrescriptionSummaryDTO>> pendingPrescriptions() {
        return ResponseEntity.ok(medicineService.getPendingPrescriptionsForClinic(myClinicId()));
    }

    /** One prescription's detail (no price yet) - for reviewing before billing. */
    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<PrescriptionSummaryDTO> prescriptionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getPrescriptionSummaryForClinic(myClinicId(), id));
    }

    /**
     * Create the bill: choose how much of each prescribed medicine to
     * actually dispense (can be less than prescribed - e.g. the patient can
     * only afford part of it) and charge for it. Deducts stock and computes
     * price server-side; the response is selling-price-only, never cost/profit.
     */
    @PostMapping("/prescriptions/{id}/bill")
    public ResponseEntity<BillReceiptDTO> billPrescription(@PathVariable Long id,
                                                             @Valid @RequestBody BillPrescriptionRequest req) {
        return ResponseEntity.ok(medicineService.billPrescription(
                CurrentUser.organizationId(), myClinicId(), CurrentUser.userId(), id, req));
    }

    /** Reprint/view the receipt for an already-billed prescription. */
    @GetMapping("/prescriptions/{id}/bill")
    public ResponseEntity<BillReceiptDTO> billReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getBillReceipt(myClinicId(), id));
    }

    private Long myClinicId() {
        ClinicAdmin admin = clinicAdminRepository.findByUserId(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic admin profile not found"));
        return admin.getClinicId();
    }
}
