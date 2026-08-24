package com.doctorapp.service;

import com.doctorapp.dto.AppointmentDTO;
import com.doctorapp.dto.DoctorAppointmentDTO;
import com.doctorapp.dto.PatientHistoryDTO;
import com.doctorapp.entity.Appointment;
import com.doctorapp.entity.AppointmentSlot;
import com.doctorapp.entity.Doctor;
import com.doctorapp.entity.Patient;
import com.doctorapp.exception.BusinessException;
import com.doctorapp.exception.ResourceNotFoundException;
import com.doctorapp.repository.AppointmentRepository;
import com.doctorapp.repository.AppointmentSlotRepository;
import com.doctorapp.repository.DoctorRepository;
import com.doctorapp.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Booking flow: AVAILABLE -> (lock row) -> re-check status -> BOOKED + create
     * Appointment, all inside one transaction. The pessimistic write lock means a
     * second concurrent request for the same slot simply waits for this transaction
     * to finish, then sees status=BOOKED and gets a clean 409 - no double booking,
     * no lost updates. Payment is intentionally out of scope for this first version
     * (see product roadmap - Phase 2); paymentStatus defaults to NOT_REQUIRED.
     */
    @Transactional
    public AppointmentDTO bookSlot(Long patientUserId, Long slotId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        AppointmentSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        if (slot.getStatus() != AppointmentSlot.SlotStatus.AVAILABLE) {
            throw new BusinessException("This slot is no longer available. Please pick another one.");
        }

        slot.setStatus(AppointmentSlot.SlotStatus.BOOKED);
        slotRepository.save(slot);

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(slot.getDoctor())
                .clinic(slot.getClinic())
                .slot(slot)
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(Appointment.AppointmentStatus.BOOKED)
                .consultationFee(slot.getDoctor().getConsultationFee())
                .paymentStatus(Appointment.PaymentStatus.NOT_REQUIRED)
                .build());

        return toDTO(appointment);
    }

    @Transactional
    public AppointmentDTO cancel(Long patientUserId, Long appointmentId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("You can only cancel your own appointments");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.BOOKED) {
            throw new BusinessException("Only booked appointments can be cancelled");
        }

        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        slot.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        return toDTO(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> myAppointments(Long patientUserId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patient.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // --- Doctor-facing methods below ---

    @Transactional(readOnly = true)
    public List<DoctorAppointmentDTO> doctorAppointments(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(doctor.getId())
                .stream().map(this::toDoctorDTO).collect(Collectors.toList());
    }

    /**
     * A doctor marks their own appointment COMPLETED, CANCELLED, or NO_SHOW. Only
     * cancelling frees the slot back up for booking - a no-show still consumed the
     * doctor's time slot, so it stays blocked.
     */
    @Transactional
    public DoctorAppointmentDTO updateStatusByDoctor(Long doctorUserId, Long appointmentId, String statusValue) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("You can only update your own appointments");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.BOOKED) {
            throw new BusinessException("Only booked appointments can be updated");
        }

        Appointment.AppointmentStatus newStatus;
        try {
            newStatus = Appointment.AppointmentStatus.valueOf(statusValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status: " + statusValue);
        }
        if (newStatus == Appointment.AppointmentStatus.BOOKED) {
            throw new BusinessException("Cannot set status back to BOOKED");
        }

        appointment.setStatus(newStatus);

        if (newStatus == Appointment.AppointmentStatus.CANCELLED) {
            AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlot().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
            slot.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        return toDoctorDTO(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<PatientHistoryDTO> doctorPatients(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(doctor.getId());

        Map<Long, List<Appointment>> byPatient = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), LinkedHashMap::new, Collectors.toList()));

        List<PatientHistoryDTO> result = new ArrayList<>();
        for (List<Appointment> group : byPatient.values()) {
            Patient patient = group.get(0).getPatient();
            result.add(PatientHistoryDTO.builder()
                    .patientId(patient.getId())
                    .patientName(patient.getName())
                    .patientPhone(patient.getPhone())
                    .totalVisits(group.size())
                    .appointments(group.stream().map(this::toDoctorDTO).collect(Collectors.toList()))
                    .build());
        }
        return result;
    }

    private DoctorAppointmentDTO toDoctorDTO(Appointment a) {
        return DoctorAppointmentDTO.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .patientPhone(a.getPatient().getPhone())
                .clinicName(a.getClinic().getClinicName())
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus().name())
                .paymentStatus(a.getPaymentStatus().name())
                .consultationFee(a.getConsultationFee())
                .build();
    }

    private AppointmentDTO toDTO(Appointment a) {
        return AppointmentDTO.builder()
                .id(a.getId())
                .doctorName(a.getDoctor().getName())
                .clinicName(a.getClinic().getClinicName())
                .clinicAddress(a.getClinic().getAddress())
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus().name())
                .consultationFee(a.getConsultationFee())
                .paymentStatus(a.getPaymentStatus().name())
                .build();
    }
}
