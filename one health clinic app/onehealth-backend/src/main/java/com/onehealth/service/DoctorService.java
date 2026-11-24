package com.onehealth.service;

import com.onehealth.dto.*;
import com.onehealth.entity.*;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Owner-facing doctor roster management + assigning doctors to branches. */
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorClinicAssignmentRepository assignmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final SpecializationRepository specializationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public DoctorDTO registerDoctor(Long organizationId, RegisterDoctorRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("An account with this email already exists.");
        }

        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.DOCTOR)
                .organizationId(organizationId)
                .enabled(true)
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(user)
                .organizationId(organizationId)
                .name(req.getName())
                .qualification(req.getQualification())
                .experienceYears(req.getExperienceYears())
                .gender(req.getGender())
                .dob(req.getDob())
                .consultationFee(req.getConsultationFee())
                .active(true)
                .build());

        if (req.getSpecializationIds() != null && !req.getSpecializationIds().isEmpty()) {
            doctor.setSpecializations(resolveOwnedSpecializations(organizationId, req.getSpecializationIds()));
            doctor = doctorRepository.save(doctor);
        }

        if (req.getClinicIds() != null) {
            for (Long clinicId : req.getClinicIds()) {
                assignToClinic(organizationId, doctor.getId(), clinicId);
            }
        }

        return toDTO(doctor);
    }

    /** Owner edits which specializations a doctor has, after creation. */
    @Transactional
    public DoctorDTO updateSpecializations(Long organizationId, Long doctorId, List<Long> specializationIds) {
        Doctor doctor = getOwnedDoctor(organizationId, doctorId);
        Set<Specialization> resolved = specializationIds == null
                ? new HashSet<>()
                : resolveOwnedSpecializations(organizationId, specializationIds);
        doctor.setSpecializations(resolved);
        return toDTO(doctorRepository.save(doctor));
    }

    private Set<Specialization> resolveOwnedSpecializations(Long organizationId, List<Long> ids) {
        Set<Specialization> result = new HashSet<>();
        for (Long id : ids) {
            Specialization s = specializationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Specialization not found: " + id));
            if (!s.getOrganizationId().equals(organizationId)) {
                throw new AccessDeniedBusinessException("That specialization does not belong to your organization.");
            }
            result.add(s);
        }
        return result;
    }

    /**
     * Patient-facing: active doctors assigned to a given branch, as DTOs (never
     * raw entities - the Doctor entity carries a lazy OneToOne to User, which
     * carries the password hash; serializing the entity directly is both the
     * "doctors not loading for the branch" bug you hit AND a latent
     * information-leak risk. Always go through toDTO from here on.
     */
    @Transactional(readOnly = true)
    public List<DoctorDTO> getActiveDoctorsAtClinic(Long clinicId) {
        return assignmentRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                .map(DoctorClinicAssignment::getDoctor)
                .filter(Doctor::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** So a doctor can pick a branch by name when setting availability, instead of needing to know its numeric ID. */
    @Transactional(readOnly = true)
    public List<ClinicDTO> getMyAssignedClinics(Long doctorId) {
        return assignmentRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
                .map(a -> {
                    Clinic c = a.getClinic();
                    return ClinicDTO.builder()
                            .id(c.getId())
                            .clinicName(c.getClinicName())
                            .address(c.getAddress())
                            .city(c.getCity())
                            .active(c.isActive())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> listDoctors(Long organizationId) {
        return doctorRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DoctorDTO setDoctorActive(Long organizationId, Long doctorId, boolean active) {
        Doctor doctor = getOwnedDoctor(organizationId, doctorId);
        doctor.setActive(active);
        return toDTO(doctorRepository.save(doctor));
    }

    /**
     * Assigns a doctor to a branch. No approval workflow (see DoctorClinicAssignment
     * javadoc) - this is a direct owner action. We still guard against assigning a
     * doctor/clinic pair from different organizations (defense in depth beyond the
     * controller's own auth check).
     */
    @Transactional
    public void assignToClinic(Long organizationId, Long doctorId, Long clinicId) {
        Doctor doctor = getOwnedDoctor(organizationId, doctorId);
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        if (!clinic.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This clinic does not belong to your organization.");
        }

        assignmentRepository.findByDoctorIdAndClinicId(doctorId, clinicId).ifPresentOrElse(
                existing -> {
                    existing.setActive(true);
                    assignmentRepository.save(existing);
                },
                () -> assignmentRepository.save(DoctorClinicAssignment.builder()
                        .doctor(doctor)
                        .clinic(clinic)
                        .active(true)
                        .build())
        );
    }

    @Transactional
    public void unassignFromClinic(Long organizationId, Long doctorId, Long clinicId) {
        getOwnedDoctor(organizationId, doctorId); // ownership check
        assignmentRepository.findByDoctorIdAndClinicId(doctorId, clinicId)
                .ifPresent(a -> {
                    a.setActive(false);
                    assignmentRepository.save(a);
                });
    }

    public Doctor getOwnedDoctor(Long organizationId, Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        if (!doctor.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This doctor does not belong to your organization.");
        }
        return doctor;
    }

    private DoctorDTO toDTO(Doctor doctor) {
        List<ClinicDTO> clinics = assignmentRepository.findByDoctorIdAndActiveTrue(doctor.getId()).stream()
                .map(a -> {
                    Clinic c = a.getClinic();
                    return ClinicDTO.builder()
                            .id(c.getId())
                            .clinicName(c.getClinicName())
                            .address(c.getAddress())
                            .city(c.getCity())
                            .active(c.isActive())
                            .build();
                })
                .collect(Collectors.toList());

        List<SpecializationDTO> specializations = doctor.getSpecializations().stream()
                .map(s -> SpecializationDTO.builder().id(s.getId()).name(s.getName()).active(s.isActive()).build())
                .collect(Collectors.toList());

        return DoctorDTO.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specializations(specializations)
                .qualification(doctor.getQualification())
                .experienceYears(doctor.getExperienceYears())
                .gender(doctor.getGender())
                .consultationFee(doctor.getConsultationFee())
                .active(doctor.isActive())
                .assignedClinics(clinics)
                .build();
    }
}
