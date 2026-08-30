package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.LedgerEntryDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.entity.Client;
import com.enterprise.ca.entity.ChartOfAccount;
import com.enterprise.ca.entity.LedgerEntry;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.LedgerEntryMapper;
import com.enterprise.ca.repository.ChartOfAccountRepository;
import com.enterprise.ca.repository.ClientRepository;
import com.enterprise.ca.repository.LedgerEntryRepository;
import com.enterprise.ca.repository.spec.LedgerEntrySpecifications;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.LedgerEntryService;
import com.enterprise.ca.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/*
 * PURPOSE: Posts and queries the general ledger. GST math lives here (not on
 * the frontend) so every entry - whether typed directly or auto-posted from
 * an invoice - is computed the same way: gstAmount = amount * rate / 100,
 * totalAmount = amount + gstAmount.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LedgerEntryServiceImpl implements LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final ClientRepository clientRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final AuditService auditService;

    @Override
    public LedgerEntryDTO post(LedgerEntryDTO dto, String createdBy) {
        ChartOfAccount account = chartOfAccountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Ledger head not found: " + dto.getAccountId()));

        Client client = null;
        if (dto.getClientId() != null) {
            client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + dto.getClientId()));
        }

        BigDecimal gstRate = dto.getGstRate() != null ? dto.getGstRate() : BigDecimal.ZERO;
        BigDecimal gstAmount = dto.getAmount().multiply(gstRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = dto.getAmount().add(gstAmount);

        LedgerEntry entry = LedgerEntry.builder()
                .client(client)
                .account(account)
                .entryType(LedgerEntry.EntryType.valueOf(dto.getEntryType()))
                .entryDate(dto.getEntryDate())
                .amount(dto.getAmount())
                .gstRate(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(total)
                .description(dto.getDescription())
                .referenceNumber(dto.getReferenceNumber())
                .reconciled(false)
                .createdBy(createdBy)
                .build();

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        auditService.log("POST", "LedgerEntry", saved.getId(),
                "Posted " + account.getName() + " entry of " + total);
        return ledgerEntryMapper.toDTO(saved);
    }

    @Override
    public LedgerEntryDTO toggleReconciled(Long id) {
        LedgerEntry entry = ledgerEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found: " + id));
        entry.setReconciled(!Boolean.TRUE.equals(entry.getReconciled()));
        return ledgerEntryMapper.toDTO(ledgerEntryRepository.save(entry));
    }

    @Override
    public void delete(Long id) {
        if (!ledgerEntryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ledger entry not found: " + id);
        }
        ledgerEntryRepository.deleteById(id);
        auditService.log("DELETE", "LedgerEntry", id, "Deleted ledger entry");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryDTO> search(Long clientId, String accountType, LocalDate start, LocalDate end,
                                                Boolean reconciled, Pageable pageable) {
        Specification<LedgerEntry> spec = Specification.where(LedgerEntrySpecifications.clientEquals(clientId))
                .and(LedgerEntrySpecifications.accountTypeEquals(accountType))
                .and(LedgerEntrySpecifications.dateBetween(start, end))
                .and(LedgerEntrySpecifications.reconciledEquals(reconciled));
        Page<LedgerEntry> page = ledgerEntryRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(ledgerEntryMapper::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSearch(Long clientId, String accountType, LocalDate start, LocalDate end, Boolean reconciled) {
        Specification<LedgerEntry> spec = Specification.where(LedgerEntrySpecifications.clientEquals(clientId))
                .and(LedgerEntrySpecifications.accountTypeEquals(accountType))
                .and(LedgerEntrySpecifications.dateBetween(start, end))
                .and(LedgerEntrySpecifications.reconciledEquals(reconciled));
        List<LedgerEntry> entries = ledgerEntryRepository.findAll(spec);

        String[] headers = {"Date", "Client", "Ledger Head", "Type", "Amount", "GST Rate %", "GST Amount",
                "Total", "Reference", "Reconciled", "Description"};
        return ExcelExportUtil.toXlsx("Ledger", headers, entries, e -> new Object[]{
                e.getEntryDate().toString(),
                e.getClient() != null ? e.getClient().getName() : "",
                e.getAccount().getName(),
                e.getEntryType().name(),
                e.getAmount(),
                e.getGstRate(),
                e.getGstAmount(),
                e.getTotalAmount(),
                e.getReferenceNumber(),
                Boolean.TRUE.equals(e.getReconciled()) ? "Yes" : "No",
                e.getDescription()
        });
    }
}
