package com.onehealth.service;

import com.onehealth.dto.*;
import com.onehealth.entity.*;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Owner-only HR management for staff (ClinicAdmin and Doctor - both are
 * employees of the organization, per the requirement that a clinic authority
 * "is the employee of the organisation" too, not just doctors). Every method
 * here is reachable only via OwnerEmployeeController (hasRole('OWNER')) -
 * salary data in particular should never leak into a DoctorDTO or any
 * response another role can see.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final ClinicAdminRepository clinicAdminRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorClinicAssignmentRepository assignmentRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public List<EmployeeListItemDTO> listEmployees(Long organizationId) {
        List<EmployeeListItemDTO> result = new ArrayList<>();

        for (ClinicAdmin admin : clinicAdminRepository.findByOrganizationId(organizationId)) {
            result.add(EmployeeListItemDTO.builder()
                    .userId(admin.getUser().getId())
                    .name(admin.getName())
                    .role("CLINIC_ADMIN")
                    .clinicSummary(clinicSummaryForAdmin(admin))
                    .profileComplete(employeeProfileRepository.findByUserId(admin.getUser().getId()).isPresent())
                    .currentSalary(currentSalaryFor(admin.getUser().getId()))
                    .build());
        }

        for (Doctor doctor : doctorRepository.findByOrganizationId(organizationId)) {
            result.add(EmployeeListItemDTO.builder()
                    .userId(doctor.getUser().getId())
                    .name(doctor.getName())
                    .role("DOCTOR")
                    .clinicSummary(clinicSummaryForDoctor(doctor))
                    .profileComplete(employeeProfileRepository.findByUserId(doctor.getUser().getId()).isPresent())
                    .currentSalary(currentSalaryFor(doctor.getUser().getId()))
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public EmployeeProfileDTO getProfile(Long organizationId, Long userId) {
        User user = getOwnedEmployeeUser(organizationId, userId);
        EmployeeProfile profile = employeeProfileRepository.findByUserId(userId).orElse(null);

        List<SalaryRecordDTO> history = profile == null ? List.of() :
                salaryRecordRepository.findByEmployeeProfileIdOrderByEffectiveFromDesc(profile.getId()).stream()
                        .map(r -> SalaryRecordDTO.builder()
                                .id(r.getId()).amount(r.getAmount())
                                .effectiveFrom(r.getEffectiveFrom()).createdAt(r.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        SalaryRecordDTO current = history.stream()
                .filter(r -> !r.getEffectiveFrom().isAfter(LocalDate.now()))
                .findFirst().orElse(null);

        String name;
        String role = user.getRole().name();
        List<String> clinicNames;
        if (user.getRole() == User.Role.CLINIC_ADMIN) {
            ClinicAdmin admin = clinicAdminRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinic admin profile not found"));
            name = admin.getName();
            clinicNames = List.of(clinicSummaryForAdmin(admin));
        } else {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
            name = doctor.getName();
            clinicNames = assignmentRepository.findByDoctorIdAndActiveTrue(doctor.getId()).stream()
                    .map(a -> a.getClinic().getClinicName())
                    .collect(Collectors.toList());
        }

        return EmployeeProfileDTO.builder()
                .userId(userId)
                .name(name)
                .role(role)
                .clinicNames(clinicNames)
                .gender(profile == null ? null : profile.getGender())
                .dob(profile == null ? null : profile.getDob())
                .dateOfJoining(profile == null ? null : profile.getDateOfJoining())
                .permanentAddress(profile == null ? null : profile.getPermanentAddress())
                .currentAddress(profile == null ? null : profile.getCurrentAddress())
                .currentSalary(current == null ? null : current.getAmount())
                .currentSalaryEffectiveFrom(current == null ? null : current.getEffectiveFrom())
                .salaryHistory(history)
                .build();
    }

    @Transactional
    public EmployeeProfileDTO upsertProfile(Long organizationId, Long userId, UpdateEmployeeProfileRequest req) {
        getOwnedEmployeeUser(organizationId, userId); // validates userId is this org's staff

        EmployeeProfile profile = employeeProfileRepository.findByUserId(userId)
                .orElseGet(() -> EmployeeProfile.builder().organizationId(organizationId).userId(userId).build());

        profile.setGender(req.getGender());
        profile.setDob(req.getDob());
        profile.setDateOfJoining(req.getDateOfJoining());
        profile.setPermanentAddress(req.getPermanentAddress());
        profile.setCurrentAddress(req.getCurrentAddress());
        employeeProfileRepository.save(profile);

        return getProfile(organizationId, userId);
    }

    /** Appends a new salary revision - never overwrites an existing one, so history is preserved. */
    @Transactional
    public EmployeeProfileDTO addSalaryRecord(Long organizationId, Long userId, AddSalaryRecordRequest req) {
        getOwnedEmployeeUser(organizationId, userId);

        if (req.getAmount().signum() < 0) {
            throw new BusinessException("Salary amount can't be negative.");
        }

        EmployeeProfile profile = employeeProfileRepository.findByUserId(userId)
                .orElseGet(() -> employeeProfileRepository.save(
                        EmployeeProfile.builder().organizationId(organizationId).userId(userId).build()));

        salaryRecordRepository.save(SalaryRecord.builder()
                .employeeProfileId(profile.getId())
                .amount(req.getAmount())
                .effectiveFrom(req.getEffectiveFrom())
                .build());

        return getProfile(organizationId, userId);
    }

    private java.math.BigDecimal currentSalaryFor(Long userId) {
        return employeeProfileRepository.findByUserId(userId)
                .map(p -> salaryRecordRepository.findByEmployeeProfileIdOrderByEffectiveFromDesc(p.getId()).stream()
                        .filter(r -> !r.getEffectiveFrom().isAfter(LocalDate.now()))
                        .findFirst()
                        .map(SalaryRecord::getAmount)
                        .orElse(null))
                .orElse(null);
    }

    private String clinicSummaryForAdmin(ClinicAdmin admin) {
        return clinicRepository.findById(admin.getClinicId())
                .map(Clinic::getClinicName)
                .orElse("Unknown branch");
    }

    private String clinicSummaryForDoctor(Doctor doctor) {
        List<String> names = assignmentRepository.findByDoctorIdAndActiveTrue(doctor.getId()).stream()
                .map(a -> a.getClinic().getClinicName())
                .collect(Collectors.toList());
        return names.isEmpty() ? "Unassigned" : String.join(", ", names);
    }

    private User getOwnedEmployeeUser(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!organizationId.equals(user.getOrganizationId())) {
            throw new AccessDeniedBusinessException("This user does not belong to your organization.");
        }
        if (user.getRole() != User.Role.CLINIC_ADMIN && user.getRole() != User.Role.DOCTOR) {
            throw new BusinessException("HR profiles are only for clinic authority and doctor staff.");
        }
        return user;
    }
}
