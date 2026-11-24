package com.enterprise.ems.constant;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * The hiring pipeline's allowed moves live in ONE place (ApplicationStage#canMoveTo). This test pins
 * the full 7x7 transition table, so a future change to the rules is a deliberate, visible test edit.
 * Pure unit test - no Spring context.
 */
class ApplicationStageTest {

    private static Set<ApplicationStage> allowedFrom(ApplicationStage from) {
        return switch (from) {
            case APPLIED -> EnumSet.of(ApplicationStage.SCREENING, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN);
            case SCREENING -> EnumSet.of(ApplicationStage.INTERVIEW, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN);
            case INTERVIEW -> EnumSet.of(ApplicationStage.OFFER, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN);
            case OFFER -> EnumSet.of(ApplicationStage.HIRED, ApplicationStage.INTERVIEW, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN);
            case REJECTED -> EnumSet.of(ApplicationStage.SCREENING);
            case HIRED, WITHDRAWN -> EnumSet.noneOf(ApplicationStage.class);
        };
    }

    @Test
    void transitionTableMatchesTheDesign() {
        for (ApplicationStage from : ApplicationStage.values()) {
            for (ApplicationStage to : ApplicationStage.values()) {
                boolean expected = allowedFrom(from).contains(to);
                assertEquals(expected, from.canMoveTo(to), "move " + from + " -> " + to);
            }
        }
    }

    @Test
    void movingToTheSameStageOrNullIsNeverAllowed() {
        for (ApplicationStage stage : ApplicationStage.values()) {
            assertFalse(stage.canMoveTo(stage), stage + " -> itself");
            assertFalse(stage.canMoveTo(null), stage + " -> null");
        }
    }

    @Test
    void hiredAndWithdrawnAreDeadEnds() {
        for (ApplicationStage to : ApplicationStage.values()) {
            assertFalse(ApplicationStage.HIRED.canMoveTo(to));
            assertFalse(ApplicationStage.WITHDRAWN.canMoveTo(to));
        }
    }

    @Test
    void aRejectedCandidateCanOnlyBeReopenedIntoScreening() {
        assertTrue(ApplicationStage.REJECTED.canMoveTo(ApplicationStage.SCREENING));
        assertFalse(ApplicationStage.REJECTED.canMoveTo(ApplicationStage.INTERVIEW));
        assertFalse(ApplicationStage.REJECTED.canMoveTo(ApplicationStage.OFFER));
        assertFalse(ApplicationStage.REJECTED.canMoveTo(ApplicationStage.HIRED));
    }

    @Test
    void terminalStagesAreExactlyHiredRejectedWithdrawn() {
        assertEquals(EnumSet.of(ApplicationStage.HIRED, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN),
                EnumSet.copyOf(java.util.Arrays.stream(ApplicationStage.values()).filter(ApplicationStage::isTerminal).toList()));
    }
}
