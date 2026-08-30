package com.orgsite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Used both for the admin editing their own org, and the public page (public endpoint just omits nothing sensitive - there's no sensitive field here). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private Long id;
    private String name;
    private String slug;
    private String category;
    private String tagline;
    private String description;
    private String address;
    private String phone;
    private String whatsapp;
    private String email;
    private String mapEmbedUrl;
    private String facebookUrl;
    private String instagramUrl;
    private String websiteUrl;
    private String logoUrl;
    private String coverImageUrl;
    private String themeColor;
    private String hoursText;
    private boolean published;
}
