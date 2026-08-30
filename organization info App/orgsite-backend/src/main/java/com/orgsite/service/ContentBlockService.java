package com.orgsite.service;

import com.orgsite.dto.ContentBlockDTO;
import com.orgsite.entity.ContentBlock;
import com.orgsite.entity.Organization;
import com.orgsite.exception.BadRequestException;
import com.orgsite.exception.ResourceNotFoundException;
import com.orgsite.repository.ContentBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentBlockService {

    private final ContentBlockRepository contentBlockRepository;
    private final OrganizationService organizationService;

    public List<ContentBlock> listForOrg(Long orgId) {
        return contentBlockRepository.findByOrganizationIdOrderBySortOrderAsc(orgId);
    }

    public List<ContentBlock> listVisibleForOrg(Long orgId) {
        return contentBlockRepository.findByOrganizationIdAndVisibleTrueOrderBySortOrderAsc(orgId);
    }

    @Transactional
    public ContentBlock create(Long orgId, ContentBlockDTO dto) {
        Organization org = organizationService.getById(orgId);
        ContentBlock block = ContentBlock.builder()
                .organization(org)
                .type(parseType(dto.getType()))
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .price(dto.getPrice())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .visible(dto.isVisible())
                .build();
        return contentBlockRepository.save(block);
    }

    @Transactional
    public ContentBlock update(Long orgId, Long blockId, ContentBlockDTO dto) {
        ContentBlock block = getOwnedBlock(orgId, blockId);

        if (dto.getType() != null) block.setType(parseType(dto.getType()));
        if (dto.getTitle() != null) block.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) block.setSubtitle(dto.getSubtitle());
        if (dto.getDescription() != null) block.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) block.setImageUrl(dto.getImageUrl());
        if (dto.getPrice() != null) block.setPrice(dto.getPrice());
        if (dto.getSortOrder() != null) block.setSortOrder(dto.getSortOrder());
        block.setVisible(dto.isVisible());

        return contentBlockRepository.save(block);
    }

    @Transactional
    public void delete(Long orgId, Long blockId) {
        ContentBlock block = getOwnedBlock(orgId, blockId);
        contentBlockRepository.delete(block);
    }

    /** Loads a block and verifies it belongs to the requesting org - the tenant isolation check. */
    private ContentBlock getOwnedBlock(Long orgId, Long blockId) {
        ContentBlock block = contentBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Content block not found"));
        if (!block.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Content block not found");
        }
        return block;
    }

    private ContentBlock.BlockType parseType(String type) {
        try {
            return ContentBlock.BlockType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid content block type: " + type);
        }
    }

    public ContentBlockDTO toDTO(ContentBlock block) {
        return ContentBlockDTO.builder()
                .id(block.getId())
                .type(block.getType().name())
                .title(block.getTitle())
                .subtitle(block.getSubtitle())
                .description(block.getDescription())
                .imageUrl(block.getImageUrl())
                .price(block.getPrice())
                .sortOrder(block.getSortOrder())
                .visible(block.isVisible())
                .build();
    }

    public List<ContentBlockDTO> toDTOList(List<ContentBlock> blocks) {
        return blocks.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
