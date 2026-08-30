package com.orgsite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single flexible content item on an organization's page: a gallery photo,
 * a menu/service item, a testimonial, an announcement, etc. Using one flexible
 * table instead of a separate entity per content type keeps the whole platform
 * generic across business categories (a restaurant's "menu item" and a school's
 * "facility" are structurally the same thing: title + description + image + price/optional).
 */
@Entity
@Table(name = "content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockType type;

    @Column(length = 150)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 30)
    private String price; // free text so "₹120" / "$8.50" / "Starting ₹5000/mo" all work

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean visible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum BlockType {
        GALLERY,       // a photo in the gallery/portfolio strip
        ITEM,          // menu item / product / service / facility - has optional price
        TESTIMONIAL,   // customer quote (title = author name, description = quote)
        ANNOUNCEMENT   // notice / offer / event banner
    }
}
