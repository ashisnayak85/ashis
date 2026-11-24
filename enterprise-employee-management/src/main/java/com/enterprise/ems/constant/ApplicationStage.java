package com.enterprise.ems.constant;

/**
 * Pipeline stage of a candidate's application to one requisition.
 *
 * The allowed moves live HERE, in one place, instead of being scattered across
 * services as if-statements. Two targets are never reachable through a plain
 * "move" call and are driven by their own flows:
 *   OFFER - entered automatically when an offer is created
 *   HIRED - entered automatically when an accepted offer is converted to an employee
 */
public enum ApplicationStage {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED,
    WITHDRAWN;

    public boolean canMoveTo(ApplicationStage target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case APPLIED -> target == SCREENING || target == REJECTED || target == WITHDRAWN;
            case SCREENING -> target == INTERVIEW || target == REJECTED || target == WITHDRAWN;
            case INTERVIEW -> target == OFFER || target == REJECTED || target == WITHDRAWN;
            case OFFER -> target == HIRED || target == INTERVIEW || target == REJECTED || target == WITHDRAWN;
            // A rejected candidate can be re-opened (talent-pool style) back into screening.
            case REJECTED -> target == SCREENING;
            case HIRED, WITHDRAWN -> false;
        };
    }

    public boolean isTerminal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN;
    }

    /** True for stages that count as "still in play" for dashboards. */
    public boolean isActive() {
        return !isTerminal();
    }
}
