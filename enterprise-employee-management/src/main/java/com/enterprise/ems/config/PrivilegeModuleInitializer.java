package com.enterprise.ems.config;

import com.enterprise.ems.constant.SystemModule;
import com.enterprise.ems.entity.PrivilegeModule;
import com.enterprise.ems.repository.PrivilegeModuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Registers a PrivilegeModule row for every SystemModule entry, on
 * EVERY application startup - not just the first (unlike DataInitializer,
 * which seeds once and stops). This is deliberately idempotent and
 * additive-only:
 *   - Runs every boot, so a SystemModule entry added in a later release
 *     (e.g. LEAVE) gets its row created automatically the next time the
 *     app starts, with no manual step.
 *   - Never updates or deletes an existing row - if admin has deactivated
 *     RESIGNATION, or renamed its display name, this leaves that alone.
 *     It only fills in what's MISSING.
 *
 * This is the replacement for admin (or a developer) manually INSERTing
 * into privilege_module - see PrivilegeModuleService / SystemModule
 * javadocs for the full reasoning. A new module now ships as a code change
 * (add to SystemModule) in the same release as the feature that uses it,
 * and the database catches up automatically on deploy.
 */
@Component
@RequiredArgsConstructor
public class PrivilegeModuleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeModuleInitializer.class);

    private final PrivilegeModuleRepository privilegeModuleRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (SystemModule module : SystemModule.values()) {
            if (privilegeModuleRepository.existsByCodeIgnoreCase(module.code)) {
                continue; // already registered - leave whatever admin has done to it alone
            }
            privilegeModuleRepository.save(PrivilegeModule.builder()
                    .code(module.code)
                    .name(module.displayName)
                    .description(module.description)
                    .active(true)
                    .build());
            log.info("Registered privilege module '{}' ({})", module.code, module.displayName);
        }
    }
}
