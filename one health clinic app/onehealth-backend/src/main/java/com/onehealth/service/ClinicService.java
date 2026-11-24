package com.onehealth.service;

import com.onehealth.dto.ClinicDTO;
import com.onehealth.dto.ClinicRequest;
import com.onehealth.dto.RegisterClinicAdminRequest;
import com.onehealth.entity.Clinic;
import com.onehealth.entity.ClinicAdmin;
import com.onehealth.entity.User;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.ClinicAdminRepository;
import com.onehealth.repository.ClinicRepository;
import com.onehealth.repository.DoctorClinicAssignmentRepository;
import com.onehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Owner-facing clinic (branch) management, and each branch's front-desk login. */
@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicAdminRepository clinicAdminRepository;
    private final DoctorClinicAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClinicDTO createClinic(Long organizationId, ClinicRequest req) {
        Clinic clinic = clinicRepository.save(Clinic.builder()
                .organizationId(organizationId)
                .clinicName(req.getClinicName())
                .address(req.getAddress())
                .city(req.getCity())
                .pincode(req.getPincode())
                .phone(req.getPhone())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .active(true)
                .build());
        return toDTO(clinic);
    }

    public List<ClinicDTO> listClinics(Long organizationId) {
        return clinicRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClinicDTO setClinicActive(Long organizationId, Long clinicId, boolean active) {
        Clinic clinic = getOwnedClinic(organizationId, clinicId);
        clinic.setActive(active);
        return toDTO(clinicRepository.save(clinic));
    }

    @Transactional
    public void registerClinicAdmin(Long organizationId, RegisterClinicAdminRequest req) {
        Clinic clinic = getOwnedClinic(organizationId, req.getClinicId());

        if (clinicAdminRepository.findByClinicId(clinic.getId()).isPresent()) {
            throw new BusinessException("This branch already has a clinic authority account. " +
                    "Disable/replace the existing one before creating another.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("An account with this email already exists.");
        }

        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.CLINIC_ADMIN)
                .organizationId(organizationId)
                .enabled(true)
                .build());

        clinicAdminRepository.save(ClinicAdmin.builder()
                .user(user)
                .organizationId(organizationId)
                .clinicId(clinic.getId())
                .name(req.getName())
                .phone(req.getPhone())
                .build());
    }

    /** Fetches a clinic and throws if it doesn't belong to this organization - the tenant-isolation check. */
    public Clinic getOwnedClinic(Long organizationId, Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        if (!clinic.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This clinic does not belong to your organization.");
        }
        return clinic;
    }

    private ClinicDTO toDTO(Clinic clinic) {
        String adminName = clinicAdminRepository.findByClinicId(clinic.getId())
                .map(ClinicAdmin::getName).orElse(null);
        int doctorCount = assignmentRepository.findByClinicIdAndActiveTrue(clinic.getId()).size();

        return ClinicDTO.builder()
                .id(clinic.getId())
                .clinicName(clinic.getClinicName())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .pincode(clinic.getPincode())
                .phone(clinic.getPhone())
                .latitude(clinic.getLatitude())
                .longitude(clinic.getLongitude())
                .active(clinic.isActive())
                .clinicAdminName(adminName)
                .doctorCount(doctorCount)
                .build();
    }
}
