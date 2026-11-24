package com.enterprise.ems.repository.spec;

import com.enterprise.ems.entity.Attendance;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/*
 * PURPOSE: Builds a dynamic JPA Specification for the attendance search/filter
 * endpoint. Every filter is optional - only the criteria that were actually
 * supplied by the caller get added to the query, so a single method covers
 * "just the date range", "just the employee", "everything combined", etc.
 */
public final class AttendanceSpecifications {

    private AttendanceSpecifications() {
        // Utility class - prevent instantiation
    }

    public static Specification<Attendance> filterBy(Long employeeId, LocalDate startDate, LocalDate endDate, String status) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (employeeId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (startDate != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
            }
            if (endDate != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
            }
            if (StringUtils.hasText(status)) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            return predicates;
        };
    }
}
