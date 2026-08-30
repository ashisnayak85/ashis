package com.enterprise.ca.repository;

import com.enterprise.ca.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long>, JpaSpecificationExecutor<LedgerEntry> {

    List<LedgerEntry> findTop10ByOrderByEntryDateDescIdDesc();

    @Query("select coalesce(sum(l.totalAmount), 0) from LedgerEntry l " +
           "where l.account.accountType = 'INCOME' and l.entryDate between :start and :end")
    BigDecimal sumIncome(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select coalesce(sum(l.totalAmount), 0) from LedgerEntry l " +
           "where l.account.accountType = 'EXPENSE' and l.entryDate between :start and :end")
    BigDecimal sumExpense(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select coalesce(sum(l.gstAmount), 0) from LedgerEntry l " +
           "where l.account.accountType = 'INCOME' and l.entryDate between :start and :end")
    BigDecimal sumOutputGst(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select coalesce(sum(l.gstAmount), 0) from LedgerEntry l " +
           "where l.account.accountType = 'EXPENSE' and l.entryDate between :start and :end")
    BigDecimal sumInputGst(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
