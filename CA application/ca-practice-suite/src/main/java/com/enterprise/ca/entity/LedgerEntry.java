package com.enterprise.ca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * PURPOSE: A single posted transaction - the core bookkeeping record. Every
 * Invoice, once marked SENT/PAID, posts one of these automatically; staff can
 * also post entries directly (expenses, journal adjustments, opening
 * balances). GST fields let the reconciliation/dashboard work out input vs
 * output tax without a separate GST module.
 */
@Entity
@Table(name = "ledger_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 14, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    // True once this entry has been checked against the bank statement /
    // GSTR-2B, matching what a CA does during monthly reconciliation.
    @Column(nullable = false)
    @Builder.Default
    private Boolean reconciled = false;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EntryType {
        DEBIT, CREDIT
    }
}
