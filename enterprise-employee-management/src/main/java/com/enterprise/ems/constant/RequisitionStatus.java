package com.enterprise.ems.constant;

/** Lifecycle of a job requisition (manpower request). */
public enum RequisitionStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    OPEN,
    ON_HOLD,
    CLOSED,
    REJECTED,
    CANCELLED;

    /** Statuses in which candidates can still be worked on. */
    public boolean isActivePipeline() {
        return this == OPEN || this == ON_HOLD;
    }
}
