package com.orgsite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlockDTO {
    private Long id;
    private String type; // GALLERY, ITEM, TESTIMONIAL, ANNOUNCEMENT
    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;
    private String price;
    private Integer sortOrder;
    private boolean visible;
}
