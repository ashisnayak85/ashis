package com.doctorapp.service;

import com.doctorapp.dto.AvailabilityRequest;
import com.doctorapp.dto.AvailabilityResponse;
import com.doctorapp.dto.SlotDTO;
import com.doctorapp.entity.AppointmentSlot;
import com.doctorapp.entity.Clinic;
import com.doctorapp.entity.Doctor;
import com.doctorapp.entity.DoctorAvailability;
import com.doctorapp.entity.DoctorClinicAssociation.Status;
import com.doctorapp.exception.BusinessException;
import com.doctorapp.exception.ResourceNotFoundException;
import com.doctorapp.repository.AppointmentSlotRepository;
import com.doctorapp.repository.ClinicRepository;
import com.doctorapp.repository.DoctorAvailabilityRepository;
import com.doctorapp.repository.DoctorClinicAssociationRepository;
import com.doctorapp.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorClinicAssociationRepository associationRepository;

    @Transactional
    public AvailabilityResponse addAvailability(Long doctorId, AvailabilityRequest req) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Clinic clinic = clinicRepository.findById(req.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + req.getClinicId()));

        // A doctor can only set working hours at a clinic once both sides have agreed
        // to the association (see DoctorClinicAssociation) - otherwise a doctor could
        // list hours at a clinic that never approved them, or vice versa.
        if (!associationRepository.existsByDoctorIdAndClinicIdAndStatus(doctorId, req.getClinicId(), Status.APPROVED)) {
            throw new BusinessException("You're not an approved doctor at this clinic yet.");
        }

        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .clinic(clinic)
                .dayOfWeek(req.getDayOfWeek())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .slotDurationMinutes(req.getSlotDurationMinutes() == null ? 15 : req.getSlotDurationMinutes())
                .build();
        DoctorAvailability saved = availabilityRepository.save(availability);

        return AvailabilityResponse.builder()
                .id(saved.getId())
                .doctorId(doctorId)
                .clinicId(clinic.getId())
                .dayOfWeek(saved.getDayOfWeek())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .slotDurationMinutes(saved.getSlotDurationMinutes())
                .active(saved.isActive())
                .build();
    }

    /**
     * Returns the bookable slots for a doctor/clinic/date, generating them from the
     * weekly availability template the first time this date is requested. Generation
     * is idempotent (unique constraint on doctor+clinic+date+start_time) so concurrent
     * requests for the same date can't create duplicates.
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
        for (DoctorAvailability template : templates) {
            LocalTime cursor = template.getStartTime();
            while (cursor.plusMinutes(template.getSlotDurationMinutes()).compareTo(template.getEndTime()) <= 0) {
                LocalTime slotEnd = cursor.plusMinutes(template.getSlotDurationMinutes());
                try {
                    AppointmentSlot slot = slotRepository.save(AppointmentSlot.builder()
                            .doctor(doctor)
                            .clinic(clinic)
                            .date(date)
                            .startTime(cursor)
                            .endTime(slotEnd)
                            .status(AppointmentSlot.SlotStatus.AVAILABLE)
                            .build());
                    created.add(slot);
                } catch (DataIntegrityViolationException ignored) {
                    // Another concurrent request already generated this slot - fine, skip it.
                }
                cursor = slotEnd;
            }
        }
        return created;
    }
}
