package com.enterprise.ems.constant;

/** State of one frozen approval stage row (requisition or offer). */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    // No eligible approver could be resolved for the stage at submit time.
    SKIPPED
}
