package com.onehealth.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper so controllers/services don't repeat the SecurityContext dance. */
public final class CurrentUser {

    private CurrentUser() {}

    public static UserPrincipal get() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static Long organizationId() {
        return get().getOrganizationId();
    }

    public static Long userId() {
        return get().getId();
    }

    /** String form of the current user's role (e.g. "OWNER", "CLINIC_ADMIN") - used for audit logging. */
    public static String role() {
        return get().getRole().name();
    }
}
