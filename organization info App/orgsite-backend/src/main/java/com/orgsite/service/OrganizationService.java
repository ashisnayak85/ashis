package com.orgsite.service;

import com.orgsite.dto.OrganizationDTO;
import com.orgsite.entity.Organization;
import com.orgsite.exception.BadRequestException;
import com.orgsite.exception.ResourceNotFoundException;
import com.orgsite.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    public Organization getBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No organization found for \"" + slug + "\""));
    }

    @Transactional
    public Organization update(Long orgId, OrganizationDTO dto) {
        Organization org = getById(orgId);

        if (dto.getName() != null) org.setName(dto.getName());
        if (dto.getTagline() != null) org.setTagline(dto.getTagline());
        if (dto.getDescription() != null) org.setDescription(dto.getDescription());
        if (dto.getAddress() != null) org.setAddress(dto.getAddress());
        if (dto.getPhone() != null) org.setPhone(dto.getPhone());
        if (dto.getWhatsapp() != null) org.setWhatsapp(dto.getWhatsapp());
        if (dto.getEmail() != null) org.setEmail(dto.getEmail());
        if (dto.getMapEmbedUrl() != null) org.setMapEmbedUrl(dto.getMapEmbedUrl());
        if (dto.getFacebookUrl() != null) org.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null) org.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getWebsiteUrl() != null) org.setWebsiteUrl(dto.getWebsiteUrl());
        if (dto.getLogoUrl() != null) org.setLogoUrl(dto.getLogoUrl());
        if (dto.getCoverImageUrl() != null) org.setCoverImageUrl(dto.getCoverImageUrl());
        if (dto.getThemeColor() != null) org.setThemeColor(dto.getThemeColor());
        if (dto.getHoursText() != null) org.setHoursText(dto.getHoursText());

        if (dto.getCategory() != null) {
            try {
                org.setCategory(Organization.Category.valueOf(dto.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid category: " + dto.getCategory());
            }
        }

        return organizationRepository.save(org);
    }

    @Transactional
    public Organization setPublished(Long orgId, boolean published) {
        Organization org = getById(orgId);
        org.setPublished(published);
        return organizationRepository.save(org);
    }

    public OrganizationDTO toDTO(Organization org) {
        return OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .category(org.getCategory().name())
                .tagline(org.getTagline())
                .description(org.getDescription())
                .address(org.getAddress())
                .phone(org.getPhone())
                .whatsapp(org.getWhatsapp())
                .email(org.getEmail())
                .mapEmbedUrl(org.getMapEmbedUrl())
                .facebookUrl(org.getFacebookUrl())
                .instagramUrl(org.getInstagramUrl())
                .websiteUrl(org.getWebsiteUrl())
                .logoUrl(org.getLogoUrl())
                .coverImageUrl(org.getCoverImageUrl())
                .themeColor(org.getThemeColor())
                .hoursText(org.getHoursText())
                .published(org.isPublished())
                .build();
    }
}
