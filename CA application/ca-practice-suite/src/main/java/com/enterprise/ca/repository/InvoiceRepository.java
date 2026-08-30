package com.enterprise.ca.repository;

import com.enterprise.ca.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    boolean existsByInvoiceNumber(String invoiceNumber);
    long countByStatusIn(List<Invoice.InvoiceStatus> statuses);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.status in :statuses")
    BigDecimal sumTotalByStatusIn(@Param("statuses") Collection<Invoice.InvoiceStatus> statuses);

    long countByClientId(Long clientId);
}
