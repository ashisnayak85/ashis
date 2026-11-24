package com.enterprise.ems.repository.spec;

import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.LeaveMaster;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/*
 * PURPOSE: Composable, optional-parameter filters for leave search.
 * Used by both the admin "search all leaves" endpoint and the self-service
 * "my leaves" endpoint, so date-range/status semantics stay identical in
 * both places instead of drifting apart.
 *
 * Every predicate here is null-safe: if the filter value is absent, it
 * degrades to cb.conjunction() (i.e. "always true" / no-op), so callers
 * can freely chain all of them together regardless of which params were
 * actually supplied by the client.
 */
public final class LeaveSpecifications {

    private LeaveSpecifications() {
        // Utility class - prevent instantiation
    }

    public static Specification<LeaveMaster> hasStatus(String status) {
        return (root, query, cb) -> (status == null || status.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("status"), status.toUpperCase());
    }

    public static Specification<LeaveMaster> forEmployee(Long employeeId) {
        return (root, query, cb) -> employeeId == null
                ? cb.conjunction()
                : cb.equal(root.get("employee").get("id"), employeeId);
    }

    /*
     * "Date range" = which leave records were active/available during that
     * window, not just which ones started inside it. A leave spanning
     * Dec 28 - Jan 3 must show up for a Jan 1 - Jan 5 filter even though its
     * startDate is outside the window. Overlap check:
     *   leave.startDate <= filterTo  AND  leave.endDate >= filterFrom
     * Either bound may be omitted for an open-ended range.
     */
    public static Specification<LeaveMaster> dateRangeOverlaps(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), to),
                        cb.greaterThanOrEqualTo(root.get("endDate"), from));
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("endDate"), from);
            }
            return cb.lessThanOrEqualTo(root.get("startDate"), to);
        };
    }

    /* Matches against first or last name; admin-only (free-text employee search). */
    public static Specification<LeaveMaster> employeeNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return cb.conjunction();
            }
            var employee = root.join("employee");
            String pattern = "%" + name.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(employee.get("firstName")), pattern),
                    cb.like(cb.lower(employee.get("lastName")), pattern));
        };
    }
}
