package com.enterprise.ca.repository.spec;

import com.enterprise.ca.entity.ComplianceTask;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class ComplianceTaskSpecifications {

    public static Specification<ComplianceTask> clientEquals(Long clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<ComplianceTask> statusEquals(String status) {
        return (root, query, cb) -> (status == null || status.isBlank()) ? null :
                cb.equal(root.get("status"), ComplianceTask.TaskStatus.valueOf(status));
    }

    public static Specification<ComplianceTask> taskTypeEquals(String taskType) {
        return (root, query, cb) -> (taskType == null || taskType.isBlank()) ? null :
                cb.equal(root.get("taskType"), ComplianceTask.TaskType.valueOf(taskType));
    }

    public static Specification<ComplianceTask> dueBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null) return cb.between(root.get("dueDate"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("dueDate"), start);
            return cb.lessThanOrEqualTo(root.get("dueDate"), end);
        };
    }
}
