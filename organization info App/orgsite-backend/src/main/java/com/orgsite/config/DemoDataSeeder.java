package com.orgsite.config;

import com.orgsite.entity.ContentBlock;
import com.orgsite.entity.Organization;
import com.orgsite.entity.User;
import com.orgsite.repository.ContentBlockRepository;
import com.orgsite.repository.OrganizationRepository;
import com.orgsite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds one example published organization ("Sunrise Tea House") on first boot,
 * purely so you can see the public page working immediately without signing up
 * first. Safe to delete this class for production - it only runs if the DB is empty.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ContentBlockRepository contentBlockRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            return;
        }

        Organization org = Organization.builder()
                .name("Sunrise Tea House")
                .slug("sunrise-tea-house")
                .category(Organization.Category.CAFE_TEA_SHOP)
                .tagline("Handbrewed chai, fresh every hour")
                .description("A cozy neighborhood tea shop serving traditional masala chai, filter coffee, and light snacks since 2015. Come sit, relax, and enjoy a proper cup of tea.")
                .address("12 MG Road, Bengaluru, Karnataka")
                .phone("+91 98765 43210")
                .whatsapp("+91 98765 43210")
                .email("hello@sunriseteahouse.example")
                .themeColor("#b45309")
                .hoursText("Mon-Sat: 7:00 AM - 9:00 PM | Sun: 8:00 AM - 6:00 PM")
                .published(true)
                .build();
        org = organizationRepository.save(org);

        User owner = User.builder()
                .email("owner@sunriseteahouse.example")
                .password(passwordEncoder.encode("Demo@1234"))
                .role(User.Role.OWNER)
                .organization(org)
                .build();
        userRepository.save(owner);

        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.ITEM)
                .title("Masala Chai").description("Our signature spiced tea, brewed with cardamom, ginger and cloves.").price("₹30").sortOrder(1).visible(true).build());
        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.ITEM)
                .title("Filter Coffee").description("South Indian style filter coffee, strong and frothy.").price("₹40").sortOrder(2).visible(true).build());
        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.ITEM)
                .title("Bun Maska").description("Soft buttered bun, a perfect chai companion.").price("₹25").sortOrder(3).visible(true).build());

        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.TESTIMONIAL)
                .title("Rekha S.").description("Best chai in the neighborhood, hands down. I stop by every morning before work.").sortOrder(1).visible(true).build());
        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.TESTIMONIAL)
                .title("Arjun M.").description("Cozy place, friendly staff, and the bun maska is unbeatable.").sortOrder(2).visible(true).build());

        contentBlockRepository.save(ContentBlock.builder().organization(org).type(ContentBlock.BlockType.ANNOUNCEMENT)
                .title("Weekend Special").description("Free samosa with every large chai, Saturdays and Sundays only.").sortOrder(1).visible(true).build());

        System.out.println("=================================================================");
        System.out.println("Demo organization seeded: /sunrise-tea-house");
        System.out.println("Demo owner login -> email: owner@sunriseteahouse.example  password: Demo@1234");
        System.out.println("=================================================================");
    }
}
