package com.enterprise.ems.repository.spec;

import com.enterprise.ems.entity.TicketMaster;
import org.springframework.data.jpa.domain.Specification;

/*
 * PURPOSE: Composable, optional-parameter filters for ticket search - same
 * pattern as LeaveSpecifications. Every predicate degrades to cb.conjunction()
 * ("always true") when its filter value is absent, so callers can freely
 * chain all of them regardless of which params the client actually sent.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
        // Utility class - prevent instantiation
    }

    public static Specification<TicketMaster> hasStatus(String status) {
        return (root, query, cb) -> (status == null || status.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("status"), status.toUpperCase());
    }

    public static Specification<TicketMaster> hasPriority(String priority) {
        return (root, query, cb) -> (priority == null || priority.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("priority"), priority.toUpperCase());
    }

    public static Specification<TicketMaster> inDepartment(Long departmentId) {
        return (root, query, cb) -> departmentId == null
                ? cb.conjunction()
                : cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<TicketMaster> raisedBy(Long employeeId) {
        return (root, query, cb) -> employeeId == null
                ? cb.conjunction()
                : cb.equal(root.get("raisedBy").get("id"), employeeId);
    }

    public static Specification<TicketMaster> assignedTo(Long employeeId) {
        return (root, query, cb) -> employeeId == null
                ? cb.conjunction()
                : cb.equal(root.get("assignedTo").get("id"), employeeId);
    }

    // Unclaimed - no one has assigned/claimed this ticket yet.
    public static Specification<TicketMaster> isUnassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignedTo"));
    }

    // Restricts to a specific set of departments - used for the claimable
    // pool, where the set is "every department this employee's ticket team
    // membership covers". An empty/null list means the employee is on no
    // ticket team anywhere, so the pool must be empty (not "everything").
    public static Specification<TicketMaster> inDepartments(java.util.List<Long> departmentIds) {
        return (root, query, cb) -> (departmentIds == null || departmentIds.isEmpty())
                ? cb.disjunction()
                : root.get("department").get("id").in(departmentIds);
    }

    /* Matches the human-friendly ticket number or the title (admin/manager free-text search). */
    public static Specification<TicketMaster> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("ticketNumber")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern));
        };
    }
}
