package com.onehealth.service;

import com.onehealth.dto.SpecializationDTO;
import com.onehealth.dto.SpecializationRequest;
import com.onehealth.entity.Specialization;
import com.onehealth.exception.AccessDeniedBusinessException;
import com.onehealth.exception.BusinessException;
import com.onehealth.exception.ResourceNotFoundException;
import com.onehealth.repository.DoctorRepository;
import com.onehealth.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Owner-managed master list of specializations - see Specialization entity javadoc. */
@Service
@RequiredArgsConstructor
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public SpecializationDTO create(Long organizationId, SpecializationRequest req) {
        String name = req.getName().trim();
        if (specializationRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw new BusinessException("A specialization named \"" + name + "\" already exists.");
        }
        Specialization s = specializationRepository.save(Specialization.builder()
                .organizationId(organizationId)
                .name(name)
                .active(true)
                .build());
        return toDTO(s);
    }

    public List<SpecializationDTO> list(Long organizationId) {
        return specializationRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpecializationDTO rename(Long organizationId, Long id, String newName) {
        Specialization s = getOwned(organizationId, id);
        String trimmed = newName.trim();
        if (!trimmed.equalsIgnoreCase(s.getName())
                && specializationRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, trimmed)) {
            throw new BusinessException("A specialization named \"" + trimmed + "\" already exists.");
        }
        s.setName(trimmed);
        return toDTO(specializationRepository.save(s));
    }

    @Transactional
    public SpecializationDTO setActive(Long organizationId, Long id, boolean active) {
        Specialization s = getOwned(organizationId, id);
        s.setActive(active);
        return toDTO(specializationRepository.save(s));
    }

    /**
     * Hard delete only if no doctor currently references it - otherwise deleting
     * would silently strip that specialization off every doctor who has it,
     * which is surprising. Deactivating (setActive(false)) is the safe default;
     * this is for cleaning up a genuine mistake (e.g. a duplicate/typo) where
     * nothing has been assigned yet.
     */
    @Transactional
    public void delete(Long organizationId, Long id) {
        Specialization s = getOwned(organizationId, id);
        boolean inUse = !doctorRepository.findByOrganizationId(organizationId).stream()
                .filter(d -> d.getSpecializations().stream().anyMatch(sp -> sp.getId().equals(id)))
                .toList().isEmpty();
        if (inUse) {
            throw new BusinessException(
                    "This specialization is assigned to one or more doctors. Deactivate it instead of deleting, " +
                    "or remove it from those doctors first.");
        }
        specializationRepository.delete(s);
    }

    private Specialization getOwned(Long organizationId, Long id) {
        Specialization s = specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found: " + id));
        if (!s.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedBusinessException("This specialization does not belong to your organization.");
        }
        return s;
    }

    private SpecializationDTO toDTO(Specialization s) {
        return SpecializationDTO.builder().id(s.getId()).name(s.getName()).active(s.isActive()).build();
    }
}
