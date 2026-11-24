package com.onehealth.exception;

/** Cross-tenant / cross-branch access attempt - e.g. a clinic admin requesting
 * another clinic's data, or a request for an org a user doesn't belong to. */
public class AccessDeniedBusinessException extends RuntimeException {
    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}
