package com.orgsite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Everything the public page needs in one call: org profile + its visible content blocks. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOrgResponse {
    private OrganizationDTO organization;
    private List<ContentBlockDTO> gallery;
    private List<ContentBlockDTO> items;
    private List<ContentBlockDTO> testimonials;
    private List<ContentBlockDTO> announcements;
}
