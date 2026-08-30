package com.onehealth.service;

import com.onehealth.dto.CreateOrganizationRequest;
import com.onehealth.entity.Organization;
import com.onehealth.entity.User;
import com.onehealth.exception.BusinessException;
import com.onehealth.repository.OrganizationRepository;
import com.onehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Platform-operator actions: onboarding new clinic-chain customers. */
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Organization createOrganization(CreateOrganizationRequest req) {
        if (organizationRepository.existsBySlug(req.getOrganizationSlug())) {
            throw new BusinessException("That organization slug is already taken.");
        }
        if (userRepository.existsByEmail(req.getOwnerEmail())) {
            throw new BusinessException("An account with this owner email already exists.");
        }

        Organization org = organizationRepository.save(Organization.builder()
                .name(req.getOrganizationName())
                .slug(req.getOrganizationSlug())
                .supportPhone(req.getSupportPhone())
                .active(true)
                .build());

        userRepository.save(User.builder()
                .email(req.getOwnerEmail())
                .password(passwordEncoder.encode(req.getOwnerPassword()))
                .role(User.Role.OWNER)
                .organizationId(org.getId())
                .name(req.getOwnerName())
                .enabled(true)
                .build());

        return org;
    }

    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional
    public Organization setOrganizationActive(Long id, boolean active) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Organization not found"));
        org.setActive(active);
        return organizationRepository.save(org);
    }
}
