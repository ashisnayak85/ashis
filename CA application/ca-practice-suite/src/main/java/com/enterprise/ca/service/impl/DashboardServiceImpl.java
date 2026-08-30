package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.DashboardStats;
import com.enterprise.ca.entity.ComplianceTask;
import com.enterprise.ca.entity.Invoice;
import com.enterprise.ca.mapper.ComplianceTaskMapper;
import com.enterprise.ca.mapper.LedgerEntryMapper;
import com.enterprise.ca.repository.*;
import com.enterprise.ca.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/*
 * PURPOSE: The one-screen answer to "how is the practice / the business
 * doing right now" - the numbers a CA would pull together manually every
 * month-end: YTD income/expense, net GST payable (output collected minus
 * input credit), what's overdue on the compliance calendar, and what's
 * still unpaid on the invoice register.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final ComplianceTaskRepository complianceTaskRepository;
    private final ComplianceTaskMapper complianceTaskMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    @Override
    public DashboardStats getStats() {
        LocalDate fyStart = currentFinancialYearStart();
        LocalDate today = LocalDate.now();

        BigDecimal income = ledgerEntryRepository.sumIncome(fyStart, today);
        BigDecimal expense = ledgerEntryRepository.sumExpense(fyStart, today);
        BigDecimal outputGst = ledgerEntryRepository.sumOutputGst(fyStart, today);
        BigDecimal inputGst = ledgerEntryRepository.sumInputGst(fyStart, today);

        List<ComplianceTask.TaskStatus> openStatuses = List.of(
                ComplianceTask.TaskStatus.PENDING, ComplianceTask.TaskStatus.IN_PROGRESS);

        return DashboardStats.builder()
                .totalClients(clientRepository.count())
                .activeClients(clientRepository.countByActiveTrue())
                .totalIncome(income)
                .totalExpense(expense)
                .netPosition(income.subtract(expense))
                .gstCollected(outputGst)
                .gstInputCredit(inputGst)
                .gstPayable(outputGst.subtract(inputGst))
                .pendingComplianceTasks(complianceTaskRepository.countByStatus(ComplianceTask.TaskStatus.PENDING)
                        + complianceTaskRepository.countByStatus(ComplianceTask.TaskStatus.IN_PROGRESS))
                .overdueComplianceTasks(complianceTaskRepository.countByStatus(ComplianceTask.TaskStatus.OVERDUE))
                .unpaidInvoices(invoiceRepository.countByStatusIn(List.of(Invoice.InvoiceStatus.SENT, Invoice.InvoiceStatus.OVERDUE)))
                .unpaidInvoiceTotal(invoiceRepository.sumTotalByStatusIn(List.of(Invoice.InvoiceStatus.SENT, Invoice.InvoiceStatus.OVERDUE)))
                .upcomingDeadlines(complianceTaskRepository.findTop10ByStatusInOrderByDueDateAsc(
                        List.of(ComplianceTask.TaskStatus.PENDING, ComplianceTask.TaskStatus.IN_PROGRESS, ComplianceTask.TaskStatus.OVERDUE))
                        .stream().map(complianceTaskMapper::toDTO).toList())
                .recentEntries(ledgerEntryRepository.findTop10ByOrderByEntryDateDescIdDesc()
                        .stream().map(ledgerEntryMapper::toDTO).toList())
                .build();
    }

    // Indian financial year: 1 April - 31 March
    private LocalDate currentFinancialYearStart() {
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        return LocalDate.of(year, 4, 1);
    }
}
