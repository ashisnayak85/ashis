package com.enterprise.ems.constant;

/*
 * PURPOSE: The single source of truth for "which privilege modules does
 * this application know about". Each entry's code MUST exactly match the
 * AppConstants.MODULE_CODE_* constant that the corresponding module's
 * service (e.g. ResignationServiceImpl) hardcodes when it filters
 * WorkflowStageRepository - see AppConstants.MODULE_CODE_RESIGNATION.
 *
 * WHY THIS EXISTS: admin can no longer create PrivilegeModule rows through
 * the API (see PrivilegeModuleService javadoc) because a module's `code` is
 * a contract with Java code, not a free business decision - a typo or a
 * renamed code here would silently break approval routing for that module.
 * Instead, PrivilegeModuleInitializer reads this enum on every application
 * startup and makes sure a matching row exists in privilege_module. Adding
 * a new module is therefore a code change (add an entry here, in the same
 * pull request as the feature code that consumes it), never a manual SQL
 * INSERT and never an admin API call.
 *
 * TO ADD A NEW MODULE (e.g. when building Leave):
 *   1. Add AppConstants.MODULE_CODE_LEAVE = "LEAVE".
 *   2. Add LEAVE(AppConstants.MODULE_CODE_LEAVE, "Leave Request", "...") below.
 *   3. Have LeaveServiceImpl filter WorkflowStageRepository by that same
 *      constant, exactly like ResignationServiceImpl does today.
 *   4. Ship all three changes together. On next startup,
 *      PrivilegeModuleInitializer creates the privilege_module row
 *      automatically - admin then configures its chain via Workflow Setup.
 */
public enum SystemModule {

    RESIGNATION(
            AppConstants.MODULE_CODE_RESIGNATION,
            "Resignation",
            "Employee resignation / separation approval workflow"
    ),

    REQUISITION(
            AppConstants.MODULE_CODE_REQUISITION,
            "Job Requisition",
            "Approval workflow for new hiring requisitions (manpower requests)"
    ),

    OFFER(
            AppConstants.MODULE_CODE_OFFER,
            "Job Offer",
            "Approval workflow for candidate job offers"
    );

    // Future modules go here, e.g.:
    // LEAVE(AppConstants.MODULE_CODE_LEAVE, "Leave Request", "Employee leave approval workflow"),

    public final String code;
    public final String displayName;
    public final String description;

    SystemModule(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
}
