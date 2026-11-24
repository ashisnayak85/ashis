package com.enterprise.ems.constant;

import java.util.EnumSet;
import java.util.Set;

public enum OfferStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    SENT,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    REVOKED,
    // Accepted offer that has been turned into an Employee record.
    CONVERTED;

    /** Offers that are still "live" - a second offer for the same application is not allowed while one of these exists. */
    public static final Set<OfferStatus> OPEN_STATUSES =
            EnumSet.of(DRAFT, PENDING_APPROVAL, APPROVED, SENT, ACCEPTED);

    public boolean isOpen() {
        return OPEN_STATUSES.contains(this);
    }
}
