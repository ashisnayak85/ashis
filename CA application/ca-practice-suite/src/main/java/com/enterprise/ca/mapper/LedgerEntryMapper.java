package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.LedgerEntryDTO;
import com.enterprise.ca.entity.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntryDTO toDTO(LedgerEntry e) {
        return LedgerEntryDTO.builder()
                .id(e.getId())
                .clientId(e.getClient() != null ? e.getClient().getId() : null)
                .clientName(e.getClient() != null ? e.getClient().getName() : null)
                .accountId(e.getAccount().getId())
                .accountName(e.getAccount().getName())
                .accountType(e.getAccount().getAccountType().name())
                .entryType(e.getEntryType().name())
                .entryDate(e.getEntryDate())
                .amount(e.getAmount())
                .gstRate(e.getGstRate())
                .gstAmount(e.getGstAmount())
                .totalAmount(e.getTotalAmount())
                .description(e.getDescription())
                .referenceNumber(e.getReferenceNumber())
                .reconciled(e.getReconciled())
                .createdBy(e.getCreatedBy())
                .build();
    }
}
