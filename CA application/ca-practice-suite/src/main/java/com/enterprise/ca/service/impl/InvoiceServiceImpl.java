package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.InvoiceDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.entity.*;
import com.enterprise.ca.exception.BusinessException;
import com.enterprise.ca.exception.DuplicateResourceException;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.InvoiceMapper;
import com.enterprise.ca.repository.*;
import com.enterprise.ca.repository.spec.InvoiceSpecifications;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/*
 * PURPOSE: Manages the invoice register and keeps the general ledger in sync
 * with it. The accounting rule this encodes: an invoice only affects the
 * books once it's actually SENT (recognised) or PAID - a DRAFT invoice is
 * not yet a transaction. Moving to SENT or PAID for the first time posts
 * exactly one LedgerEntry (idempotent via the ledgerEntry link on Invoice);
 * cancelling never reverses it automatically - that's a deliberate manual
 * reversal entry, same as a real accountant would do.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private static final Set<String> POSTING_STATUSES = Set.of("SENT", "PAID");

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InvoiceMapper invoiceMapper;
    private final AuditService auditService;

    @Override
    public InvoiceDTO create(InvoiceDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + dto.getClientId()));

        String invoiceNumber = dto.getInvoiceNumber();
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            invoiceNumber = generateInvoiceNumber(dto.getInvoiceType());
        } else if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw new DuplicateResourceException("Invoice number already exists: " + invoiceNumber);
        }

        BigDecimal gstRate = dto.getGstRate() != null ? dto.getGstRate() : BigDecimal.ZERO;
        BigDecimal gstAmount = dto.getSubtotal().multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = dto.getSubtotal().add(gstAmount);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .client(client)
                .invoiceType(Invoice.InvoiceType.valueOf(dto.getInvoiceType()))
                .invoiceDate(dto.getInvoiceDate())
                .dueDate(dto.getDueDate())
                .description(dto.getDescription())
                .subtotal(dto.getSubtotal())
                .gstRate(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(total)
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        auditService.log("CREATE", "Invoice", saved.getId(), "Created invoice " + saved.getInvoiceNumber());
        return invoiceMapper.toDTO(saved);
    }

    @Override
    public InvoiceDTO updateStatus(Long id, String status, String actor) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        Invoice.InvoiceStatus newStatus = Invoice.InvoiceStatus.valueOf(status);

        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new BusinessException("Cannot change the status of a cancelled invoice");
        }

        invoice.setStatus(newStatus);

        if (POSTING_STATUSES.contains(newStatus.name()) && invoice.getLedgerEntry() == null) {
            LedgerEntry entry = postLedgerEntryForInvoice(invoice, actor);
            invoice.setLedgerEntry(entry);
        }

        Invoice updated = invoiceRepository.save(invoice);
        auditService.log("STATUS_CHANGE", "Invoice", id, "Invoice " + invoice.getInvoiceNumber() + " -> " + status);
        return invoiceMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO getById(Long id) {
        return invoiceMapper.toDTO(invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id)));
    }

    @Override
    public void delete(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (invoice.getLedgerEntry() != null) {
            throw new BusinessException("Cannot delete an invoice that has already posted to the ledger - cancel it instead");
        }
        invoiceRepository.delete(invoice);
        auditService.log("DELETE", "Invoice", id, "Deleted draft invoice " + invoice.getInvoiceNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceDTO> search(Long clientId, String type, String status, Pageable pageable) {
        Specification<Invoice> spec = Specification.where(InvoiceSpecifications.clientEquals(clientId))
                .and(InvoiceSpecifications.typeEquals(type))
                .and(InvoiceSpecifications.statusEquals(status));
        Page<Invoice> page = invoiceRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(invoiceMapper::toDTO));
    }

    private LedgerEntry postLedgerEntryForInvoice(Invoice invoice, String actor) {
        // SALES invoices post to the "Sales Revenue" income head (money coming in);
        // PURCHASE invoices post to "Purchases / Expenses" (money going out).
        // These codes are seeded by DataInitializer.
        String accountCode = invoice.getInvoiceType() == Invoice.InvoiceType.SALES ? "SALES" : "PURCH";
        ChartOfAccount account = chartOfAccountRepository.findAll().stream()
                .filter(a -> a.getCode().equals(accountCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Default ledger head '" + accountCode + "' is missing - contact an admin"));

        LedgerEntry entry = LedgerEntry.builder()
                .client(invoice.getClient())
                .account(account)
                .entryType(invoice.getInvoiceType() == Invoice.InvoiceType.SALES
                        ? LedgerEntry.EntryType.CREDIT : LedgerEntry.EntryType.DEBIT)
                .entryDate(invoice.getInvoiceDate())
                .amount(invoice.getSubtotal())
                .gstRate(invoice.getGstRate())
                .gstAmount(invoice.getGstAmount())
                .totalAmount(invoice.getTotalAmount())
                .description("Auto-posted from invoice " + invoice.getInvoiceNumber())
                .referenceNumber(invoice.getInvoiceNumber())
                .reconciled(false)
                .createdBy(actor)
                .build();
        return ledgerEntryRepository.save(entry);
    }

    private String generateInvoiceNumber(String invoiceType) {
        String prefix = "SALES".equals(invoiceType) ? "INV" : "PUR";
        String yearTag = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long seq = invoiceRepository.count() + 1;
        String candidate = prefix + "-" + yearTag + "-" + String.format("%04d", seq);
        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            seq++;
            candidate = prefix + "-" + yearTag + "-" + String.format("%04d", seq);
        }
        return candidate;
    }
}
