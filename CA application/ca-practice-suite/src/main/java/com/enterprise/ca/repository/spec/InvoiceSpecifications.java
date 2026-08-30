package com.enterprise.ca.repository.spec;

import com.enterprise.ca.entity.Invoice;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class InvoiceSpecifications {

    public static Specification<Invoice> clientEquals(Long clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Invoice> typeEquals(String type) {
        return (root, query, cb) -> (type == null || type.isBlank()) ? null :
                cb.equal(root.get("invoiceType"), Invoice.InvoiceType.valueOf(type));
    }

    public static Specification<Invoice> statusEquals(String status) {
        return (root, query, cb) -> (status == null || status.isBlank()) ? null :
                cb.equal(root.get("status"), Invoice.InvoiceStatus.valueOf(status));
    }

    public static Specification<Invoice> dateBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null) return cb.between(root.get("invoiceDate"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("invoiceDate"), start);
            return cb.lessThanOrEqualTo(root.get("invoiceDate"), end);
        };
    }
}
