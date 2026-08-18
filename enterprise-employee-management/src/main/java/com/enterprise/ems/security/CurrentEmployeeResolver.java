package com.enterprise.ems.security;

import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.exception.BusinessException;
import com.enterprise.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Central place to answer "which Employee record is this logged-in
 * user allowed to act as". Every self-service endpoint (my dashboard, my
 * leaves, my attendance) resolves the employee THROUGH this component instead
 * of trusting an employeeId sent from the client - that's what stops a plain
 * ROLE_USER account from reading or writing another employee's data by simply
 * editing the request payload.
 */
@Component
@RequiredArgsConstructor
public class CurrentEmployeeResolver {

    private final EmployeeRepository employeeRepository;

    // Looks up the Employee linked to the current session's username.
    // Throws rather than returning null/Optional so callers can't forget to check -
    // an ADMIN account with no linked Employee (e.g. a pure IT/superuser login)
    // simply isn't allowed to use self-service endpoints, and that's the correct behavior.
    public Employee requireCurrentEmployee(UserDetails principal) {
        if (principal == null) {
            throw new BusinessException("Not authenticated");
        }
        return employeeRepository.findByUserUsername(principal.getUsername())
                .orElseThrow(() -> new BusinessException(
                        "This login (" + principal.getUsername() + ") is not linked to an employee record"));
    }

    // ADMIN/MANAGER can act on behalf of any employee (pick from a dropdown).
    // A plain ROLE_USER can only ever act as themselves.
    public boolean isPrivileged(UserDetails principal) {
        return principal != null && principal.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_MANAGER"));
    }
}
