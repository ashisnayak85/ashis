package com.enterprise.ems.repository;

import com.enterprise.ems.entity.EmployeeWorkflowRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeWorkflowRoleRepository extends JpaRepository<EmployeeWorkflowRole, Long> {

    // Company-wide holders of a role (scopeDepartment IS NULL) - used to
    // resolve SPECIFIC_ROLE-style stages. Ordered by id so resolution is
    // deterministic when more than one employee holds the same role.
    List<EmployeeWorkflowRole> findByWorkflowRoleIdAndScopeDepartmentIsNullAndActiveTrueOrderByIdAsc(Long workflowRoleId);

    // Department-scoped holders of a role - used to resolve
    // ROLE_IN_DEPARTMENT-style stages (employee's own department, or a
    // fixed department).
    List<EmployeeWorkflowRole> findByWorkflowRoleIdAndScopeDepartmentIdAndActiveTrueOrderByIdAsc(Long workflowRoleId, Long scopeDepartmentId);

    // Admin "Employee Access" screen - everything currently/ever assigned to
    // one employee.
    List<EmployeeWorkflowRole> findByEmployeeIdOrderByAssignedAtDesc(Long employeeId);

    // Admin screen - current active holders of one role, across all scopes.
    List<EmployeeWorkflowRole> findByWorkflowRoleIdAndActiveTrueOrderByIdAsc(Long workflowRoleId);

    Optional<EmployeeWorkflowRole> findByEmployeeIdAndWorkflowRoleIdAndScopeDepartmentIdAndActiveTrue(
            Long employeeId, Long workflowRoleId, Long scopeDepartmentId);

    boolean existsByWorkflowRoleIdAndActiveTrue(Long workflowRoleId);

    // Hiring: does this employee currently hold the named business role (e.g. "Recruiter"), in any scope?
    boolean existsByEmployeeIdAndWorkflowRole_NameIgnoreCaseAndActiveTrue(Long employeeId, String workflowRoleName);

    // Hiring: everyone currently holding the named business role, for the recruiter picker.
    List<EmployeeWorkflowRole> findByWorkflowRole_NameIgnoreCaseAndActiveTrueOrderByIdAsc(String workflowRoleName);
}
