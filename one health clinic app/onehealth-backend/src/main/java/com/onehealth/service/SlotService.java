package com.onehealth.service;

import com.onehealth.dto.AvailabilityAuditLogDTO;
import com.onehealth.dto.AvailabilityRequest;
import com.onehealth.dto.AvailabilityResponse;
import com.onehealth.dto.SlotDTO;
import com.onehealth.dto.SlotResyncResultDTO;
import com.onehealth.entity.AppointmentSlot;
import com.onehealth.entity.AvailabilityAction;
import com.onehealth.entity.AvailabilityAuditLog;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.Doctor;
import com.onehealth.entity.DoctorAvailability;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.AppointmentSlotRepository;
import com.onehealth.repository.AvailabilityAuditLogRepository;
import com.onehealth.repository.ClinicRepository;
import com.onehealth.repository.DoctorAvailabilityRepository;
import com.onehealth.repository.DoctorClinicAssignmentRepository;
import com.onehealth.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorClinicAssignmentRepository assignmentRepository;
    private final AvailabilityAuditLogRepository auditLogRepository;
    private final UserDisplayNameService userDisplayNameService;

    /**
     * A doctor (or the owner, on a doctor's behalf) sets recurring weekly hours at
     * one of their assigned branches. Two checks matter here:
     *  1. The doctor must actually be assigned to this clinic (see
     *     DoctorClinicAssignment - no approval workflow, but assignment still has
     *     to exist).
     *  2. THE CROSS-BRANCH OVERLAP CHECK: reject any window that overlaps a window
     *     this doctor already has at ANY branch of the org on the same weekday.
     *     This is what makes "same doctor can't be booked/available at two
     *     locations at once" actually hold - it's enforced here, at template
     *     creation time, not only at individual-slot-booking time, so it's
     *     impossible to even generate colliding bookable slots in the first place.
     *
     * After saving the new template, any future dates that already have
     * generated slots (because a patient or front desk already viewed that date)
     * are re-synced so the new window shows up there too - see resyncFutureSlots.
     */
    @Transactional
    public AvailabilityResponse addAvailability(Long doctorId, AvailabilityRequest req, Long actorUserId, String actorRole) {
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException("Start time must be before end time.");
        }

        int slotDurationMinutes = req.getSlotDurationMinutes() == null ? 15 : req.getSlotDurationMinutes();
        // CRITICAL: a duration of 0 (or negative) makes the slot-generation loop
        // below (cursor.plusMinutes(duration)) never advance past its starting
        // point, which would hang the request thread forever the first time
        // anyone views a date in this window. Reject it here instead of letting
        // it reach that loop.
        if (slotDurationMinutes < 1) {
            throw new BusinessException("Slot length must be at least 1 minute.");
        }
        long windowMinutes = java.time.Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
        if (slotDurationMinutes > windowMinutes) {
            throw new BusinessException("Slot length can't be longer than the window itself.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Clinic clinic = clinicRepository.findById(req.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + req.getClinicId()));

        if (!clinic.getOrganizationId().equals(doctor.getOrganizationId())) {
            throw new AccessDeniedBusinessException("This clinic is not part of your organization.");
        }
        if (!assignmentRepository.existsByDoctorIdAndClinicIdAndActiveTrue(doctorId, req.getClinicId())) {
            throw new BusinessException("This doctor is not assigned to this branch yet.");
        }

        List<DoctorAvailability> clashes = availabilityRepository.findOverlapping(
                doctorId, req.getDayOfWeek(), req.getStartTime(), req.getEndTime());
        if (!clashes.isEmpty()) {
            DoctorAvailability clash = clashes.get(0);
            throw new BusinessException(String.format(
                    "This doctor already has %s %s-%s hours at %s. A doctor can't be scheduled " +
                    "at two branches (or double-booked at one) for an overlapping time - adjust the " +
                    "time or remove that window first.",
                    capitalize(clash.getDayOfWeek().toString()),
                    clash.getStartTime(), clash.getEndTime(), clash.getClinic().getClinicName()));
        }

        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .clinic(clinic)
                .dayOfWeek(req.getDayOfWeek())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .slotDurationMinutes(slotDurationMinutes)
                .build();
        availability = availabilityRepository.save(availability);

        logAudit(availability, AvailabilityAction.CREATED, actorUserId, actorRole, null);

        SlotResyncResultDTO resync = resyncFutureSlots(doctor, clinic, req.getDayOfWeek());
        return toResponse(availability, resync);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getMyAvailability(Long doctorId) {
        return availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .map(a -> toResponse(a, null))
                .collect(Collectors.toList());
    }

    /**
     * Every time period for every doctor at this branch, meant for the clinic
     * authority's schedule view - each row already carries doctor name, clinic
     * name, day, times, and active status, so the frontend can render the
     * on/off toggle directly against this list without a second lookup.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getClinicAvailability(Long clinicId) {
        return availabilityRepository.findByClinicIdOrderByDayOfWeekAscStartTimeAsc(clinicId).stream()
                .map(a -> toResponse(a, null))
                .collect(Collectors.toList());
    }

    /** Same as getClinicAvailability, but across every branch in the org - the owner's view. */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getOrganizationAvailability(Long organizationId, Long clinicIdOrNull) {
        List<Clinic> clinics = clinicIdOrNull != null
                ? clinicRepository.findById(clinicIdOrNull)
                        .filter(c -> c.getOrganizationId().equals(organizationId))
                        .map(List::of).orElse(List.of())
                : clinicRepository.findByOrganizationId(organizationId);

        List<AvailabilityResponse> result = new ArrayList<>();
        for (Clinic clinic : clinics) {
            result.addAll(availabilityRepository.findByClinicIdOrderByDayOfWeekAscStartTimeAsc(clinic.getId()).stream()
                    .map(a -> toResponse(a, null))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Removes a weekly window and re-syncs any already-generated future dates on
     * that weekday so slots that only existed because of this template disappear
     * from what patients/front-desk can book - except any that are already
     * BOOKED, which are left alone and reported back as a warning instead of
     * being silently cancelled out from under a real appointment.
     */
    @Transactional
    public SlotResyncResultDTO deleteAvailability(Long doctorId, Long availabilityId, Long actorUserId, String actorRole) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found: " + availabilityId));
        if (!availability.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedBusinessException("You can only remove your own availability.");
        }

        Doctor doctor = availability.getDoctor();
        Clinic clinic = availability.getClinic();
        DayOfWeek dayOfWeek = availability.getDayOfWeek();

        // Log before deleting - the row won't exist to read from afterward.
        logAudit(availability, AvailabilityAction.DELETED, actorUserId, actorRole, null);

        availabilityRepository.delete(availability);

        return resyncFutureSlots(doctor, clinic, dayOfWeek);
    }

    /** Clinic-authority-scoped toggle - restricted to their own branch. */
    @Transactional
    public AvailabilityResponse setAvailabilityActiveForClinicAdmin(Long clinicId, Long availabilityId, boolean active,
                                                                     Long actorUserId, String actorRole) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found: " + availabilityId));
        if (!availability.getClinic().getId().equals(clinicId)) {
            throw new AccessDeniedBusinessException("This availability window isn't at your clinic.");
        }
        return doSetActive(availability, active, actorUserId, actorRole);
    }

    /** Owner-scoped toggle - allowed for any branch in their organization. */
    @Transactional
    public AvailabilityResponse setAvailabilityActiveForOwner(Long organizationId, Long availabilityId, boolean active,
                                                               Long actorUserId, String actorRole) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found: " + availabilityId));
        if (!availability.getClinic().getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This availability window isn't part of your organization.");
        }
        return doSetActive(availability, active, actorUserId, actorRole);
    }

    private AvailabilityResponse doSetActive(DoctorAvailability availability, boolean active, Long actorUserId, String actorRole) {
        availability.setActive(active);
        availability = availabilityRepository.save(availability);

        SlotResyncResultDTO resync = resyncFutureSlots(
                availability.getDoctor(), availability.getClinic(), availability.getDayOfWeek());

        logAudit(availability, active ? AvailabilityAction.ACTIVATED : AvailabilityAction.DEACTIVATED,
                actorUserId, actorRole, summarizeResync(resync));

        return toResponse(availability, resync);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityAuditLogDTO> getHistoryForOrganization(Long organizationId, Long clinicIdOrNull) {
        List<AvailabilityAuditLog> logs = clinicIdOrNull != null
                ? auditLogRepository.findByOrganizationIdAndClinicIdOrderByPerformedAtDesc(organizationId, clinicIdOrNull)
                : auditLogRepository.findByOrganizationIdOrderByPerformedAtDesc(organizationId);
        return logs.stream().map(this::toAuditDTO).collect(Collectors.toList());
    }

    /**
     * Returns the bookable slots for a doctor/clinic/date, generating them from the
     * weekly template the first time this date is requested. Generation is
     * idempotent (unique constraint on doctor+clinic+date+start_time).
     *
     * For today's date, a slot is hidden only once the WHOLE recurring window it
     * belongs to has ended - not the moment its own individual end time passes.
     * E.g. for a 09:00-12:00 window generating 15-minute slots, the 9:00-9:15
     * slot is still shown/bookable at 9:20, 10:00, even 11:55 - it only
     * disappears once the clock passes 12:00. This matches how the clinic
     * actually runs (a patient who's a bit "late" for their labeled slot is
     * still seen within the same window, not turned away the instant their
     * exact minute passes) - see resolveWindowEndTime / findWindowEndTime.
     * This is a display/listing convenience only; the actual enforcement that
     * matters is in AppointmentService's booking path (lockAndValidateSlot),
     * which re-checks the same window-end cutoff at the moment of booking.
     */
    @Transactional
    public List<SlotDTO> getOrGenerateSlots(Long doctorId, Long clinicId, LocalDate date) {
        List<AppointmentSlot> existing = slotRepository
                .findByDoctorIdAndClinicIdAndDateOrderByStartTimeAsc(doctorId, clinicId, date);

        if (existing.isEmpty()) {
            existing = generateSlotsForDate(doctorId, clinicId, date);
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<DoctorAvailability> templates = availabilityRepository
                .findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(doctorId, clinicId, date.getDayOfWeek());

        return existing.stream()
                .filter(s -> s.getStatus() != AppointmentSlot.SlotStatus.CANCELLED)
                .filter(s -> !date.equals(today) || findWindowEndTime(s, templates).isAfter(now))
                .map(s -> SlotDTO.builder()
                        .slotId(s.getId())
                        .date(s.getDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .status(s.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Same cutoff rule as getOrGenerateSlots above, but for a single already-
     * fetched slot (used at actual booking time, where re-fetching the day's
     * full slot list would be wasteful for checking just one). Public because
     * AppointmentService calls this at the moment of booking - the real gate,
     * not just what's shown in a list the client may have loaded a while ago.
     */
    @Transactional(readOnly = true)
    public LocalTime resolveBookableCutoff(AppointmentSlot slot) {
        List<DoctorAvailability> templates = availabilityRepository.findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(
                slot.getDoctor().getId(), slot.getClinic().getId(), slot.getDate().getDayOfWeek());
        return findWindowEndTime(slot, templates);
    }

    /**
     * Finds which recurring window (DoctorAvailability template) a generated
     * slot belongs to and returns that window's end time - the real cutoff for
     * whether the slot can still be booked, not the slot's own narrower end
     * time. Falls back to the slot's own end time if no active template
     * matches anymore (e.g. the template was edited/deactivated after this
     * slot was generated) - resyncFutureSlots normally keeps that from
     * happening for AVAILABLE slots in the first place, so this is a safety
     * net, not the expected path.
     */
    private LocalTime findWindowEndTime(AppointmentSlot slot, List<DoctorAvailability> templates) {
        return templates.stream()
                .filter(t -> !slot.getStartTime().isBefore(t.getStartTime()) && !slot.getEndTime().isAfter(t.getEndTime()))
                .map(DoctorAvailability::getEndTime)
                .findFirst()
                .orElse(slot.getEndTime());
    }

    private List<AppointmentSlot> generateSlotsForDate(Long doctorId, Long clinicId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));

        List<DoctorAvailability> templates = availabilityRepository
                .findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(doctorId, clinicId, date.getDayOfWeek());

        List<AppointmentSlot> created = new ArrayList<>();
        for (LocalTime[] window : expandTemplatesToWindows(templates)) {
            try {
                AppointmentSlot slot = slotRepository.save(AppointmentSlot.builder()
                        .doctor(doctor)
                        .clinic(clinic)
                        .date(date)
                        .startTime(window[0])
                        .endTime(window[1])
                        .status(AppointmentSlot.SlotStatus.AVAILABLE)
                        .build());
                created.add(slot);
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent request already generated this slot - fine, skip it.
            }
        }
        return created;
    }

    /**
     * Fills the gap where a doctor's already-viewed future dates would otherwise
     * go stale after their weekly template changes (add/remove/activate/
     * deactivate). Only dates that already have at least one generated slot row
     * are touched - a future date nobody has looked at yet doesn't need fixing,
     * it will simply generate correctly from the current templates whenever it's
     * first requested (see getOrGenerateSlots).
     *
     * For each affected date:
     *  - a time window the current templates say SHOULD exist but doesn't yet
     *    (or exists only as a previously-cancelled row) is added/reactivated as AVAILABLE
     *  - a previously-generated AVAILABLE slot that the current templates no
     *    longer cover is marked CANCELLED (soft removal - keeps the row for
     *    audit/history instead of deleting it)
     *  - a BOOKED slot that the current templates no longer cover is left
     *    completely untouched and reported in `warnings` instead - a live
     *    appointment is never silently cancelled by a schedule edit.
     */
    private SlotResyncResultDTO resyncFutureSlots(Doctor doctor, Clinic clinic, DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        List<AppointmentSlot> futureSlots = slotRepository
                .findByDoctorIdAndClinicIdAndDateGreaterThanEqual(doctor.getId(), clinic.getId(), today)
                .stream()
                .filter(s -> s.getDate().getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());

        Map<LocalDate, List<AppointmentSlot>> byDate = futureSlots.stream()
                .collect(Collectors.groupingBy(AppointmentSlot::getDate));

        List<DoctorAvailability> currentTemplates = availabilityRepository
                .findByDoctorIdAndClinicIdAndDayOfWeekAndActiveTrue(doctor.getId(), clinic.getId(), dayOfWeek);
        List<LocalTime[]> expectedWindows = expandTemplatesToWindows(currentTemplates);
        Set<LocalTime> expectedStarts = expectedWindows.stream().map(w -> w[0]).collect(Collectors.toSet());

        int added = 0;
        int cancelled = 0;
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<LocalDate, List<AppointmentSlot>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            Map<LocalTime, AppointmentSlot> existingByStart = new HashMap<>();
            for (AppointmentSlot s : entry.getValue()) {
                existingByStart.put(s.getStartTime(), s);
            }

            // Add or reactivate anything the current template expects.
            for (LocalTime[] window : expectedWindows) {
                AppointmentSlot match = existingByStart.get(window[0]);
                if (match == null) {
                    try {
                        slotRepository.save(AppointmentSlot.builder()
                                .doctor(doctor).clinic(clinic).date(date)
                                .startTime(window[0]).endTime(window[1])
                                .status(AppointmentSlot.SlotStatus.AVAILABLE)
                                .build());
                        added++;
                    } catch (DataIntegrityViolationException ignored) {
                        // Race with a concurrent request - fine, skip.
                    }
                } else if (match.getStatus() == AppointmentSlot.SlotStatus.CANCELLED) {
                    match.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);
                    match.setEndTime(window[1]);
                    slotRepository.save(match);
                    added++;
                }
                // AVAILABLE / LOCKED / BOOKED and still expected -> already correct, leave it.
            }

            // Remove anything no longer expected.
            for (AppointmentSlot s : entry.getValue()) {
                if (s.getStatus() == AppointmentSlot.SlotStatus.CANCELLED) continue;
                if (expectedStarts.contains(s.getStartTime())) continue;

                if (s.getStatus() == AppointmentSlot.SlotStatus.BOOKED) {
                    warnings.add("A booked appointment on " + date + " at " + s.getStartTime()
                            + " falls outside the updated schedule. It was left as-is - cancel or "
                            + "reschedule it manually if needed.");
                } else {
                    s.setStatus(AppointmentSlot.SlotStatus.CANCELLED);
                    slotRepository.save(s);
                    cancelled++;
                }
            }
        }

        return SlotResyncResultDTO.builder()
                .futureDatesChecked(byDate.size())
                .slotsAdded(added)
                .slotsCancelled(cancelled)
                .warnings(warnings)
                .build();
    }

    /** Walks a list of weekly templates into concrete (start, end) time windows at their configured slot length. */
    private List<LocalTime[]> expandTemplatesToWindows(List<DoctorAvailability> templates) {
        List<LocalTime[]> windows = new ArrayList<>();
        for (DoctorAvailability template : templates) {
            int duration = template.getSlotDurationMinutes();
            // Defense in depth: addAvailability already rejects duration < 1, but
            // this loop would hang forever on bad data from any other path (a
            // direct DB edit, a future bulk-import script, a migrated record) -
            // skip a malformed template instead of ever looping infinitely here.
            if (duration < 1) {
                continue;
            }
            LocalTime cursor = template.getStartTime();
            while (cursor.plusMinutes(duration).compareTo(template.getEndTime()) <= 0) {
                LocalTime end = cursor.plusMinutes(duration);
                windows.add(new LocalTime[]{cursor, end});
                cursor = end;
            }
        }
        return windows;
    }

    private void logAudit(DoctorAvailability availability, AvailabilityAction action,
                           Long actorUserId, String actorRole, String note) {
        String actorName = userDisplayNameService.resolve(actorUserId);
        auditLogRepository.save(AvailabilityAuditLog.builder()
                .organizationId(availability.getClinic().getOrganizationId())
                .availabilityId(availability.getId())
                .doctorId(availability.getDoctor().getId())
                .doctorName(availability.getDoctor().getName())
                .clinicId(availability.getClinic().getId())
                .clinicName(availability.getClinic().getClinicName())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .action(action)
                .performedByUserId(actorUserId)
                .performedByName(actorName == null ? "Unknown" : actorName)
                .performedByRole(actorRole)
                .note(note)
                .build());
    }

    private String summarizeResync(SlotResyncResultDTO resync) {
        if (resync == null) return null;
        List<String> parts = new ArrayList<>();
        if (resync.getSlotsAdded() > 0) parts.add(resync.getSlotsAdded() + " slot(s) added");
        if (resync.getSlotsCancelled() > 0) parts.add(resync.getSlotsCancelled() + " slot(s) cancelled");
        if (!resync.getWarnings().isEmpty()) parts.add(resync.getWarnings().size() + " booking(s) need manual review");
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private AvailabilityResponse toResponse(DoctorAvailability a, SlotResyncResultDTO resync) {
        return AvailabilityResponse.builder()
                .id(a.getId())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .clinicId(a.getClinic().getId())
                .clinicName(a.getClinic().getClinicName())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .slotDurationMinutes(a.getSlotDurationMinutes())
                .active(a.isActive())
                .resync(resync)
                .build();
    }

    private AvailabilityAuditLogDTO toAuditDTO(AvailabilityAuditLog log) {
        return AvailabilityAuditLogDTO.builder()
                .id(log.getId())
                .doctorName(log.getDoctorName())
                .clinicName(log.getClinicName())
                .dayOfWeek(log.getDayOfWeek())
                .startTime(log.getStartTime())
                .endTime(log.getEndTime())
                .action(log.getAction().name())
                .performedByName(log.getPerformedByName())
                .performedByRole(log.getPerformedByRole())
                .note(log.getNote())
                .performedAt(log.getPerformedAt())
                .build();
    }

    private String capitalize(String s) {
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }
}
