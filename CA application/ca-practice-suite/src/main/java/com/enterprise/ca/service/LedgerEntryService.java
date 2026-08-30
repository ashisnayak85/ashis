package com.enterprise.ca.service;

import com.enterprise.ca.dto.LedgerEntryDTO;
import com.enterprise.ca.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LedgerEntryService {
    LedgerEntryDTO post(LedgerEntryDTO dto, String createdBy);
    LedgerEntryDTO toggleReconciled(Long id);
    void delete(Long id);
    PageResponse<LedgerEntryDTO> search(Long clientId, String accountType, LocalDate start, LocalDate end,
                                         Boolean reconciled, Pageable pageable);
    byte[] exportSearch(Long clientId, String accountType, LocalDate start, LocalDate end, Boolean reconciled);
}
