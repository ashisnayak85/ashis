package com.orgsite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row = one business's public marketing site.
 * The "slug" is what appears in the public URL: yoursite.com/{slug}
 */
@Entity
@Table(name = "organizations", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(length = 200)
    private String tagline;

    @Column(length = 2000)
    private String description;

    @Column(length = 300)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String whatsapp;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String mapEmbedUrl; // Google Maps embed link, optional

    @Column(length = 500)
    private String facebookUrl;

    @Column(length = 500)
    private String instagramUrl;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(length = 20)
    @Builder.Default
    private String themeColor = "#2563eb"; // hex color, used to theme the public page

    @Column(length = 500)
    private String hoursText; // free text e.g. "Mon-Sat: 9am - 9pm, Sun: Closed"

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false; // owner can keep it in draft until ready

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Category {
        RESTAURANT, CAFE_TEA_SHOP, SCHOOL, RETAIL_SHOP, SALON_SPA, GYM_FITNESS, CLINIC, OTHER
    }
}
