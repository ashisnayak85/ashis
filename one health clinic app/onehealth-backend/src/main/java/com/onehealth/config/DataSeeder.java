package com.onehealth.config;

import com.onehealth.entity.Organization;
import com.onehealth.entity.User;
import com.onehealth.repository.OrganizationRepository;
import com.onehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds:
 *  1. A SUPER_ADMIN account (platform operator - onboards new clinic-chain
 *     customers via POST /api/super-admin/organizations). Always seeded.
 *  2. Optionally, a demo Organization + its first OWNER login, controlled by
 *     app.seed.demo-org - set to false before handing this off to a real
 *     customer so they start from a clean org list.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email}")
    private String superAdminEmail;

    @Value("${app.superadmin.password}")
    private String superAdminPassword;

    @Value("${app.seed.demo-org}")
    private boolean seedDemoOrg;

    @Value("${app.seed.demo-org-name}")
    private String demoOrgName;

    @Value("${app.seed.owner-email}")
    private String ownerEmail;

    @Value("${app.seed.owner-password}")
    private String ownerPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(superAdminEmail)) {
            userRepository.save(User.builder()
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .role(User.Role.SUPER_ADMIN)
                    .organizationId(null)
                    .name("Platform Super Admin")
                    .enabled(true)
                    .build());
        }

        if (seedDemoOrg && !organizationRepository.existsBySlug(slugify(demoOrgName))) {
            Organization org = organizationRepository.save(Organization.builder()
                    .name(demoOrgName)
                    .slug(slugify(demoOrgName))
                    .active(true)
                    .build());

            if (!userRepository.existsByEmail(ownerEmail)) {
                userRepository.save(User.builder()
                        .email(ownerEmail)
                        .password(passwordEncoder.encode(ownerPassword))
                        .role(User.Role.OWNER)
                        .organizationId(org.getId())
                        .name(demoOrgName + " Owner")
                        .enabled(true)
                        .build());
            }
        }
    }

    private String slugify(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
