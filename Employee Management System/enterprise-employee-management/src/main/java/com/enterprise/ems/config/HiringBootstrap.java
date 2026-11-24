package com.enterprise.ems.config;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.entity.WorkflowRole;
import com.enterprise.ems.repository.WorkflowRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Makes sure the "Recruiter" business role exists, on EVERY startup
 * (same additive, idempotent idea as PrivilegeModuleInitializer). DataInitializer
 * can't do this - it seeds only once on an empty database, so existing installs
 * would never get the role.
 *
 * After deploying, an admin assigns "Recruiter" to HR people under
 * Employee Access (admin screen). Admins don't need it - they already have full hiring access.
 */
@Component
@RequiredArgsConstructor
public class HiringBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HiringBootstrap.class);

    private final WorkflowRoleRepository workflowRoleRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (workflowRoleRepository.existsByNameIgnoreCase(AppConstants.WORKFLOW_ROLE_RECRUITER)) {
            return; // leave whatever admin has done to it alone
        }
        workflowRoleRepository.save(WorkflowRole.builder()
                .name(AppConstants.WORKFLOW_ROLE_RECRUITER)
                .description("Can manage candidates, interviews and offers in the Hiring module")
                .active(true)
                .build());
        log.info("Created workflow role '{}' for the Hiring module", AppConstants.WORKFLOW_ROLE_RECRUITER);
    }
}
