package com.onehealth.service;

import com.onehealth.dto.AppointmentDTO;
import com.onehealth.dto.UpdateAppointmentStatusRequest;
import com.onehealth.dto.WalkInBookingRequest;
import com.onehealth.entity.Appointment;
import com.onehealth.entity.AppointmentSlot;
import com.onehealth.entity.ClinicAdmin;
import com.onehealth.entity.Patient;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ClinicAdminRepository clinicAdminRepository;

    /**
     * Online self-service booking flow: AVAILABLE -> (pessimistic lock) -> re-check
     * -> BOOKED + create Appointment(source=ONLINE), all in one transaction. The
     * lock means a second concurrent request for the same slot simply waits, then
     * sees status=BOOKED and gets a clean conflict - no double booking.
     */
    @Transactional
    public AppointmentDTO bookOnline(Long organizationId, Long patientUserId, Long slotId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        AppointmentSlot slot = lockAndValidateSlot(slotId, organizationId);

        Appointment appointment = createBookedAppointment(
                organizationId, patient, slot, Appointment.BookingSource.ONLINE, patientUserId);

        return toDTO(appointment);
    }

    /**
     * Front-desk walk-in flow: same slot-locking mechanics as online booking, but
     * initiated by a clinic admin for a patient standing at the counter. If
     * `patientId` isn't given, a new lightweight Patient record is created from
     * name/phone with no login required (see Patient.userId nullable javadoc).
     */
    @Transactional
    public AppointmentDTO bookWalkIn(Long organizationId, Long clinicAdminUserId, WalkInBookingRequest req) {
        ClinicAdmin clinicAdmin = clinicAdminRepository.findByUserId(clinicAdminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic admin profile not found"));

        AppointmentSlot slot = lockAndValidateSlot(req.getSlotId(), organizationId);

        if (!slot.getClinic().getId().equals(clinicAdmin.getClinicId())) {
            throw new AccessDeniedBusinessException("You can only book walk-ins for your own branch.");
        }

        Patient patient;
        if (req.getPatientId() != null) {
            patient = patientRepository.findById(req.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + req.getPatientId()));
            if (!patient.getOrganizationId().equals(organizationId)) {
                throw new AccessDeniedBusinessException("This patient does not belong to your organization.");
            }
        } else {
            if (req.getPatientName() == null || req.getPatientName().isBlank()) {
                throw new BusinessException("Provide either an existing patientId or a patientName for the walk-in.");
            }
            patient = patientRepository.save(Patient.builder()
                    .organizationId(organizationId)
                    .name(req.getPatientName())
                    .phone(req.getPatientPhone())
                    .createdByClinicAdminId(clinicAdmin.getId())
                    .build());
        }

        Appointment appointment = createBookedAppointment(
                organizationId, patient, slot, Appointment.BookingSource.WALK_IN, clinicAdminUserId);

        return toDTO(appointment);
    }

    @Transactional
    public AppointmentDTO cancel(Long organizationId, Long requestingUserId, Long appointmentId, boolean isPatientSelf) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This appointment does not belong to your organization.");
        }
        if (isPatientSelf && (appointment.getPatient().getUser() == null
                || !appointment.getPatient().getUser().getId().equals(requestingUserId))) {
            throw new AccessDeniedBusinessException("You can only cancel your own appointments.");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.BOOKED) {
            throw new BusinessException("Only booked appointments can be cancelled.");
        }

        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        slot.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        return toDTO(appointmentRepository.save(appointment));
    }

    /** Doctor/clinic-admin marks an appointment COMPLETED or NO_SHOW after the visit - feeds the dashboard's conversion metrics. */
    @Transactional
    public AppointmentDTO updateStatus(Long organizationId, Long appointmentId, UpdateAppointmentStatusRequest req) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (!appointment.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This appointment does not belong to your organization.");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.BOOKED) {
            throw new BusinessException("Only a currently-booked appointment's outcome can be recorded.");
        }
        appointment.setStatus(req.getStatus());
        return toDTO(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> myAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> doctorAppointments(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentDateOrderByStartTimeAsc(doctorId, date).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> clinicAppointments(Long clinicId, LocalDate date) {
        return appointmentRepository.findByClinicIdAndAppointmentDateOrderByStartTimeAsc(clinicId, date).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    private AppointmentSlot lockAndValidateSlot(Long slotId, Long organizationId) {
        AppointmentSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        if (!slot.getClinic().getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This slot does not belong to your organization.");
        }
        if (slot.getStatus() != AppointmentSlot.SlotStatus.AVAILABLE) {
            throw new BusinessException("This slot is no longer available. Please pick another one.");
        }
        return slot;
    }

    private Appointment createBookedAppointment(Long organizationId, Patient patient, AppointmentSlot slot,
                                                 Appointment.BookingSource source, Long bookedByUserId) {
        slot.setStatus(AppointmentSlot.SlotStatus.BOOKED);
        slotRepository.save(slot);

        return appointmentRepository.save(Appointment.builder()
                .organizationId(organizationId)
                .patient(patient)
                .doctor(slot.getDoctor())
                .clinic(slot.getClinic())
                .slot(slot)
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(Appointment.AppointmentStatus.BOOKED)
                .source(source)
                .bookedByUserId(bookedByUserId)
                .consultationFee(slot.getDoctor().getConsultationFee())
                .paymentStatus(Appointment.PaymentStatus.NOT_REQUIRED)
                .build());
    }

    private AppointmentDTO toDTO(Appointment a) {
        return AppointmentDTO.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .patientPhone(a.getPatient().getPhone())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .clinicId(a.getClinic().getId())
                .clinicName(a.getClinic().getClinicName())
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .source(a.getSource())
                .consultationFee(a.getConsultationFee())
                .paymentStatus(a.getPaymentStatus())
                .build();
    }
}
