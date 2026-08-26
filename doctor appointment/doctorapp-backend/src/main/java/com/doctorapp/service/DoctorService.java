package com.doctorapp.service;

import com.doctorapp.dto.ClinicSummaryDTO;
import com.doctorapp.dto.DoctorClinicAssociationDTO;
import com.doctorapp.dto.DoctorProfileDTO;
import com.doctorapp.dto.NearbyDoctorResult;
import com.doctorapp.entity.Clinic;
import com.doctorapp.entity.Doctor;
import com.doctorapp.entity.DoctorClinicAssociation;
import com.doctorapp.entity.DoctorClinicAssociation.InitiatedBy;
import com.doctorapp.entity.DoctorClinicAssociation.Status;
import com.doctorapp.entity.Specialization;
import com.doctorapp.exception.BusinessException;
import com.doctorapp.exception.ResourceNotFoundException;
import com.doctorapp.repository.ClinicRepository;
import com.doctorapp.repository.DoctorClinicAssociationRepository;
import com.doctorapp.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorClinicAssociationRepository associationRepository;

    @Value("${app.search.default-radius-km:5}")
    private double defaultRadiusKm;

    public List<NearbyDoctorResult> findNearby(double lat, double lng, Double radiusKm, String specialization) {
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : defaultRadiusKm;
        return clinicRepository.findNearbyDoctors(lat, lng, radius, specialization);
    }

    @Transactional(readOnly = true)
    public DoctorProfileDTO getProfile(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        // A doctor's clinics are now every clinic they have an APPROVED association
        // with (see DoctorClinicAssociation) - not clinics they "own".
        List<Clinic> clinics = associationRepository.findByDoctorIdAndStatus(doctorId, Status.APPROVED).stream()
                .map(DoctorClinicAssociation::getClinic)
                .collect(Collectors.toList());

        return DoctorProfileDTO.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .qualification(doctor.getQualification())
                .experienceYears(doctor.getExperienceYears())
                .consultationFee(doctor.getConsultationFee())
                .profileImageUrl(doctor.getProfileImageUrl())
                .verified(doctor.isVerified())
                .avgRating(doctor.getAvgRating())
                .ratingCount(doctor.getRatingCount())
                .specializations(doctor.getSpecializations().stream()
                        .map(Specialization::getName).collect(Collectors.toList()))
                .clinics(clinics.stream().map(c -> DoctorProfileDTO.ClinicSummary.builder()
                        .id(c.getId())
                        .clinicName(c.getClinicName())
                        .address(c.getAddress())
                        .latitude(c.getLatitude())
                        .longitude(c.getLongitude())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    /** Browse verified, active clinics that a doctor could request to join. */
    @Transactional(readOnly = true)
    public List<ClinicSummaryDTO> browseClinics(String city) {
        return clinicRepository.findByVerified(true).stream()
                .filter(Clinic::isActive)
                .filter(c -> city == null || city.isBlank() || city.equalsIgnoreCase(c.getCity()))
                .map(c -> ClinicSummaryDTO.builder()
                        .id(c.getId())
                        .clinicName(c.getClinicName())
                        .address(c.getAddress())
                        .city(c.getCity())
                        .pincode(c.getPincode())
                        .phone(c.getPhone())
                        .latitude(c.getLatitude())
                        .longitude(c.getLongitude())
                        .verified(c.isVerified())
                        .active(c.isActive())
                        .doctorCount(associationRepository.countByClinicIdAndStatus(c.getId(), Status.APPROVED))
                        .build())
                .collect(Collectors.toList());
    }

    /** All of this doctor's associations (any status) so their dashboard can show pending/approved/rejected. */
    @Transactional(readOnly = true)
    public List<DoctorClinicAssociationDTO> getMyAssociations(Long doctorId) {
        return associationRepository.findByDoctorId(doctorId).stream()
                .map(this::toAssociationDTO)
                .collect(Collectors.toList());
    }

    /** Doctor requests to join a clinic. The clinic admin must approve before it counts. */
    @Transactional
    public DoctorClinicAssociationDTO requestJoinClinic(Long doctorId, Long clinicId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));
        if (!clinic.isVerified() || !clinic.isActive()) {
            throw new BusinessException("This clinic isn't accepting doctors right now.");
        }

        DoctorClinicAssociation association = associationRepository.findByDoctorIdAndClinicId(doctorId, clinicId)
                .orElse(null);

        if (association != null && association.getStatus() == Status.APPROVED) {
            throw new BusinessException("You're already associated with this clinic.");
        }
        if (association != null && association.getStatus() == Status.PENDING) {
            throw new BusinessException("A request with this clinic is already pending.");
        }

        if (association == null) {
            association = DoctorClinicAssociation.builder()
                    .doctor(doctor)
                    .clinic(clinic)
                    .initiatedBy(InitiatedBy.DOCTOR)
                    .status(Status.PENDING)
                    .build();
        } else {
            // Re-requesting after a previous REJECTED/REMOVED association - reuse the
            // row (unique constraint on doctor_id+clinic_id) rather than inserting a duplicate.
            association.setInitiatedBy(InitiatedBy.DOCTOR);
            association.setStatus(Status.PENDING);
            association.setRespondedAt(null);
        }
        return toAssociationDTO(associationRepository.save(association));
    }

    /** Doctor approves or rejects a CLINIC-initiated invite. */
    @Transactional
    public DoctorClinicAssociationDTO respondToInvite(Long doctorId, Long associationId, boolean approve) {
        DoctorClinicAssociation association = associationRepository.findById(associationId)
                .orElseThrow(() -> new ResourceNotFoundException("Association not found: " + associationId));
        if (!association.getDoctor().getId().equals(doctorId)) {
            throw new BusinessException("This invite doesn't belong to you.");
        }
        if (association.getInitiatedBy() != InitiatedBy.CLINIC) {
            throw new BusinessException("Only the clinic can approve a request the doctor initiated.");
        }
        if (association.getStatus() != Status.PENDING) {
            throw new BusinessException("This invite has already been responded to.");
        }
        association.setStatus(approve ? Status.APPROVED : Status.REJECTED);
        association.setRespondedAt(LocalDateTime.now());
        return toAssociationDTO(associationRepository.save(association));
    }

    /** Doctor leaves a clinic they're currently associated with. */
    @Transactional
    public void leaveClinic(Long doctorId, Long associationId) {
        DoctorClinicAssociation association = associationRepository.findById(associationId)
                .orElseThrow(() -> new ResourceNotFoundException("Association not found: " + associationId));
        if (!association.getDoctor().getId().equals(doctorId)) {
            throw new BusinessException("This association doesn't belong to you.");
        }
        association.setStatus(Status.REMOVED);
        association.setRespondedAt(LocalDateTime.now());
        associationRepository.save(association);
    }

    public List<Doctor> searchBySpecialization(String specialization) {
        return doctorRepository.searchBySpecialization(specialization);
    }

    public Long getDoctorIdForUser(Long userId) {
        return doctorRepository.findByUserId(userId)
                .map(Doctor::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for this account"));
    }

    private DoctorClinicAssociationDTO toAssociationDTO(DoctorClinicAssociation a) {
        return DoctorClinicAssociationDTO.builder()
                .id(a.getId())
                .clinicId(a.getClinic().getId())
                .clinicName(a.getClinic().getClinicName())
                .clinicAddress(a.getClinic().getAddress())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .doctorQualification(a.getDoctor().getQualification())
                .initiatedBy(a.getInitiatedBy().name())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())
                .respondedAt(a.getRespondedAt())
                .build();
    }
}
