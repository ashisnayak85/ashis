package com.orgsite.controller;

import com.orgsite.dto.PublicOrgResponse;
import com.orgsite.entity.ContentBlock;
import com.orgsite.entity.Organization;
import com.orgsite.exception.ResourceNotFoundException;
import com.orgsite.service.ContentBlockService;
import com.orgsite.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Fully public, unauthenticated endpoints - this is what every visitor's browser calls. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final OrganizationService organizationService;
    private final ContentBlockService contentBlockService;

    @GetMapping("/org/{slug}")
    public ResponseEntity<PublicOrgResponse> getBySlug(@PathVariable String slug) {
        Organization org = organizationService.getBySlug(slug);
        if (!org.isPublished()) {
            throw new ResourceNotFoundException("No organization found for \"" + slug + "\"");
        }

        List<ContentBlock> blocks = contentBlockService.listVisibleForOrg(org.getId());

        PublicOrgResponse response = PublicOrgResponse.builder()
                .organization(organizationService.toDTO(org))
                .gallery(contentBlockService.toDTOList(filterByType(blocks, ContentBlock.BlockType.GALLERY)))
                .items(contentBlockService.toDTOList(filterByType(blocks, ContentBlock.BlockType.ITEM)))
                .testimonials(contentBlockService.toDTOList(filterByType(blocks, ContentBlock.BlockType.TESTIMONIAL)))
                .announcements(contentBlockService.toDTOList(filterByType(blocks, ContentBlock.BlockType.ANNOUNCEMENT)))
                .build();

        return ResponseEntity.ok(response);
    }

    private List<ContentBlock> filterByType(List<ContentBlock> blocks, ContentBlock.BlockType type) {
        return blocks.stream().filter(b -> b.getType() == type).toList();
    }
}
