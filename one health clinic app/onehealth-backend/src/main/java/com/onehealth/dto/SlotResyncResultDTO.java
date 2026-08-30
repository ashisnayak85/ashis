package com.onehealth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Reports what happened when already-generated future slots were reconciled
 * against an updated weekly template (see SlotService.resyncFutureSlots).
 * `warnings` calls out anything that needed a human to look at it - most
 * importantly, an existing BOOKED appointment that now falls outside the
 * doctor's updated hours. Those are never auto-cancelled; a live booking is
 * only ever changed by an explicit cancel/reschedule action.
 */
@Getter
@Builder
public class SlotResyncResultDTO {
    private int futureDatesChecked;
    private int slotsAdded;
    private int slotsCancelled;
    @Builder.Default
    private List<String> warnings = List.of();
}
