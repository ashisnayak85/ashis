package com.doctorapp.service;

import com.doctorapp.dto.AdminAppointmentDTO;
import com.doctorapp.dto.ClinicSummaryDTO;
import com.doctorapp.dto.DashboardStatsDTO;
import com.doctorapp.dto.DoctorSummaryDTO;
import com.doctorapp.dto.PatientSummaryDTO;
import com.doctorapp.entity.Clinic;
import com.doctorapp.entity.Doctor;
import com.doctorapp.entity.DoctorClinicAssociation.Status;
import com.doctorapp.entity.Specialization;
import com.doctorapp.exception.ResourceNotFoundException;
import com.doctorapp.repository.AppointmentRepository;
import com.doctorapp.repository.ClinicRepository;
import com.doctorapp.repository.DoctorClinicAssociationRepository;
import com.doctorapp.repository.DoctorRepository;
import com.doctorapp.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only operations: platform-wide visibility, doctor verification, and clinic
 * verification. A doctor is not publicly searchable/bookable (see
 * ClinicRepositoryCustomImpl) until an admin calls verifyDoctor() here, and the
 * same is now true for clinics via verifyClinic() - a clinic created by a clinic
 * admin doesn't show up in nearby search or accept associations until verified.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorClinicAssociationRepository associationRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .totalDoctors(doctorRepository.count())
                .verifiedDoctors(doctorRepository.countByVerified(true))
                .pendingDoctors(doctorRepository.countByVerified(false))
                .totalPatients(patientRepository.count())
                .totalAppointments(appointmentRepository.count())
                .todayAppointments(appointmentRepository.countByAppointmentDate(LocalDate.now()))
                .totalClinics(clinicRepository.count())
                .verifiedClinics(clinicRepository.countByVerified(true))
                .pendingClinics(clinicRepository.countByVerified(false))
                .build();
    }

    @Transactional(readOnly = true)
    public List<DoctorSummaryDTO> listDoctors(Boolean verified) {
        List<Doctor> doctors = verified == null
                ? doctorRepository.findAll()
                : doctorRepository.findByVerified(verified);
        return doctors.stream().map(this::toDoctorSummary).collect(Collectors.toList());
    }

    @Transactional
    public DoctorSummaryDTO verifyDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        doctor.setVerified(true);
        return toDoctorSummary(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorSummaryDTO setDoctorActive(Long doctorId, boolean active) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        doctor.setActive(active);
        return toDoctorSummary(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<ClinicSummaryDTO> listClinics(Boolean verified) {
        List<Clinic> clinics = verified == null
                ? clinicRepository.findAll()
                : clinicRepository.findByVerified(verified);
        return clinics.stream().map(this::toClinicSummary).collect(Collectors.toList());
    }

    @Transactional
    public ClinicSummaryDTO verifyClinic(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        clinic.setVerified(true);
        return toClinicSummary(clinicRepository.save(clinic));
    }

    @Transactional
    public ClinicSummaryDTO setClinicActive(Long clinicId, boolean active) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        clinic.setActive(active);
        return toClinicSummary(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> listPatients() {
        return patientRepository.findAll().stream()
                .map(p -> PatientSummaryDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getUser().getEmail())
                        .phone(p.getPhone())
                        .gender(p.getGender() == null ? null : p.getGender().name())
                        .dob(p.getDob())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminAppointmentDTO> listAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescStartTimeDesc().stream()
                .map(a -> AdminAppointmentDTO.builder()
                        .id(a.getId())
                        .patientName(a.getPatient().getName())
                        .doctorName(a.getDoctor().getName())
                        .clinicName(a.getClinic().getClinicName())
                        .appointmentDate(a.getAppointmentDate())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .status(a.getStatus().name())
                        .paymentStatus(a.getPaymentStatus().name())
                        .consultationFee(a.getConsultationFee())
                        .build())
                .collect(Collectors.toList());
    }

    private DoctorSummaryDTO toDoctorSummary(Doctor doctor) {
        // clinicCount now means "clinics this doctor has an APPROVED association with",
        // not "clinics this doctor owns" - ownership moved to ClinicAdmin.
        int clinicCount = associationRepository.findByDoctorIdAndStatus(doctor.getId(), Status.APPROVED).size();
        return DoctorSummaryDTO.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .email(doctor.getUser().getEmail())
                .qualification(doctor.getQualification())
                .experienceYears(doctor.getExperienceYears())
                .consultationFee(doctor.getConsultationFee())
                .verified(doctor.isVerified())
                .active(doctor.isActive())
                .specializations(doctor.getSpecializations().stream()
                        .map(Specialization::getName).collect(Collectors.toList()))
                .clinicCount(clinicCount)
                .createdAt(doctor.getCreatedAt())
                .build();
    }

    private ClinicSummaryDTO toClinicSummary(Clinic clinic) {
        long doctorCount = associationRepository.countByClinicIdAndStatus(clinic.getId(), Status.APPROVED);
        return ClinicSummaryDTO.builder()
                .id(clinic.getId())
                .clinicName(clinic.getClinicName())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .pincode(clinic.getPincode())
                .phone(clinic.getPhone())
                .latitude(clinic.getLatitude())
                .longitude(clinic.getLongitude())
                .verified(clinic.isVerified())
                .active(clinic.isActive())
                .doctorCount(doctorCount)
                .build();
    }
}
