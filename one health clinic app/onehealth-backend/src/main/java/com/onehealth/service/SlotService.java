package com.onehealth.service;

import com.onehealth.dto.AvailabilityRequest;
import com.onehealth.dto.AvailabilityResponse;
import com.onehealth.dto.SlotDTO;
import com.onehealth.dto.SlotResyncResultDTO;
import com.onehealth.entity.AppointmentSlot;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.Doctor;
import com.onehealth.entity.DoctorAvailability;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.AppointmentSlotRepository;
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
    public AvailabilityResponse addAvailability(Long doctorId, AvailabilityRequest req) {
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException("Start time must be before end time.");
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
                .slotDurationMinutes(req.getSlotDurationMinutes() == null ? 15 : req.getSlotDurationMinutes())
                .build();
        availability = availabilityRepository.save(availability);

        SlotResyncResultDTO resync = resyncFutureSlots(doctor, clinic, req.getDayOfWeek());
        return toResponse(availability, resync);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getMyAvailability(Long doctorId) {
        return availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .map(a -> toResponse(a, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getClinicAvailability(Long clinicId) {
        return availabilityRepository.findByClinicIdOrderByDayOfWeekAscStartTimeAsc(clinicId).stream()
                .map(a -> toResponse(a, null))
                .collect(Collectors.toList());
    }

    /**
     * Removes a weekly window and re-syncs any already-generated future dates on
     * that weekday so slots that only existed because of this template disappear
     * from what patients/front-desk can book - except any that are already
     * BOOKED, which are left alone and reported back as a warning instead of
     * being silently cancelled out from under a real appointment.
     */
    @Transactional
    public SlotResyncResultDTO deleteAvailability(Long doctorId, Long availabilityId) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found: " + availabilityId));
        if (!availability.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedBusinessException("You can only remove your own availability.");
        }

        Doctor doctor = availability.getDoctor();
        Clinic clinic = availability.getClinic();
        DayOfWeek dayOfWeek = availability.getDayOfWeek();

        availabilityRepository.delete(availability);

        return resyncFutureSlots(doctor, clinic, dayOfWeek);
    }

    @Transactional
    public AvailabilityResponse setAvailabilityActive(Long clinicId, Long availabilityId, boolean active) {
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found: " + availabilityId));
        if (!availability.getClinic().getId().equals(clinicId)) {
            throw new AccessDeniedBusinessException("This availability window isn't at your clinic.");
        }
        availability.setActive(active);
        availability = availabilityRepository.save(availability);

        SlotResyncResultDTO resync = resyncFutureSlots(
                availability.getDoctor(), availability.getClinic(), availability.getDayOfWeek());
        return toResponse(availability, resync);
    }

    /**
     * Returns the bookable slots for a doctor/clinic/date, generating them from the
     * weekly template the first time this date is requested. Generation is
     * idempotent (unique constraint on doctor+clinic+date+start_time).
     */
    @Transactional
    public List<SlotDTO> getOrGenerateSlots(Long doctorId, Long clinicId, LocalDate date) {
        List<AppointmentSlot> existing = slotRepository
                .findByDoctorIdAndClinicIdAndDateOrderByStartTimeAsc(doctorId, clinicId, date);

        if (existing.isEmpty()) {
            existing = generateSlotsForDate(doctorId, clinicId, date);
        }

        return existing.stream()
                .filter(s -> s.getStatus() != AppointmentSlot.SlotStatus.CANCELLED)
                .map(s -> SlotDTO.builder()
                        .slotId(s.getId())
                        .date(s.getDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .status(s.getStatus().name())
                        .build())
                .collect(Collectors.toList());
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
            LocalTime cursor = template.getStartTime();
            while (cursor.plusMinutes(template.getSlotDurationMinutes()).compareTo(template.getEndTime()) <= 0) {
                LocalTime end = cursor.plusMinutes(template.getSlotDurationMinutes());
                windows.add(new LocalTime[]{cursor, end});
                cursor = end;
            }
        }
        return windows;
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

    private String capitalize(String s) {
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }
}
