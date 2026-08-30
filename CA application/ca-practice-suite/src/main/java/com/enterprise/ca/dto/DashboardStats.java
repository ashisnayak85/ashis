package com.enterprise.ca.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStats {
    private long totalClients;
    private long activeClients;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netPosition;
    private BigDecimal gstPayable;     // output GST collected minus input GST paid, current FY
    private BigDecimal gstCollected;
    private BigDecimal gstInputCredit;
    private long pendingComplianceTasks;
    private long overdueComplianceTasks;
    private long unpaidInvoices;
    private BigDecimal unpaidInvoiceTotal;
    private List<ComplianceTaskDTO> upcomingDeadlines;
    private List<LedgerEntryDTO> recentEntries;
}
