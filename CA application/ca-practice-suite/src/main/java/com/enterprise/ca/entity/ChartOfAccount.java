package com.enterprise.ca.entity;

import jakarta.persistence.*;
import lombok.*;

/*
 * PURPOSE: A ledger head (chart-of-accounts line) that every LedgerEntry is
 * posted against - e.g. "Sales Revenue", "Office Rent", "Input GST Credit".
 * This is what lets the same LedgerEntry table roll up into a P&L / balance
 * sheet: group by account.accountType and sum.
 */
@Entity
@Table(name = "chart_of_account")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public enum AccountType {
        INCOME, EXPENSE, ASSET, LIABILITY, EQUITY
    }
}
