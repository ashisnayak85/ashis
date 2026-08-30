package com.enterprise.ca.repository.spec;

import com.enterprise.ca.entity.LedgerEntry;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class LedgerEntrySpecifications {

    public static Specification<LedgerEntry> clientEquals(Long clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<LedgerEntry> accountTypeEquals(String accountType) {
        return (root, query, cb) -> (accountType == null || accountType.isBlank()) ? null :
                cb.equal(root.get("account").get("accountType"), accountType);
    }

    public static Specification<LedgerEntry> dateBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null) return cb.between(root.get("entryDate"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("entryDate"), start);
            return cb.lessThanOrEqualTo(root.get("entryDate"), end);
        };
    }

    public static Specification<LedgerEntry> reconciledEquals(Boolean reconciled) {
        return (root, query, cb) -> reconciled == null ? null : cb.equal(root.get("reconciled"), reconciled);
    }
}
