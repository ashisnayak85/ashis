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
}
