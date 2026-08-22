package com.enterprise.ems.repository;

import com.enterprise.ems.entity.DepartmentTicketTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentTicketTeamRepository extends JpaRepository<DepartmentTicketTeam, Long> {

    List<DepartmentTicketTeam> findByDepartmentId(Long departmentId);

    List<DepartmentTicketTeam> findByDepartmentIdAndRoleInTeam(Long departmentId, String roleInTeam);

    boolean existsByDepartmentIdAndEmployeeIdAndRoleInTeam(Long departmentId, Long employeeId, String roleInTeam);

    // Used to authorize "can this employee view/claim tickets in this department at all".
    boolean existsByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);

    // Every department this employee is on the ticket team for (any role) -
    // drives the "claimable pool" query so a member only sees unclaimed
    // tickets from departments they're actually attached to.
    @org.springframework.data.jpa.repository.Query(
            "select t.department.id from DepartmentTicketTeam t where t.employee.id = :employeeId")
    List<Long> findDepartmentIdsByEmployeeId(Long employeeId);
}
