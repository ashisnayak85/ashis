package com.enterprise.ems.service.hiring;

import com.enterprise.ems.entity.Employee;

/*
 * PURPOSE: "Who is doing this" for the hiring module, resolved ONCE per request
 * from the signed-in user: the Employee record plus what they may do.
 *
 *  admin       - auth role ROLE_ADMIN
 *  manager     - auth role ROLE_MANAGER
 *  recruiter   - holds the "Recruiter" business (workflow) role via Employee Access
 *  staff       - admin OR recruiter = full access to hiring data (candidates, salaries, all requisitions)
 *
 * Everyone else only sees what they are personally involved in (requisitions they
 * raised / manage / approve, interviews they sit on).
 */
public record HiringActor(Employee employee, boolean admin, boolean manager, boolean recruiter) {

    public Long id() {
        return employee.getId();
    }

    public boolean staff() {
        return admin || recruiter;
    }

    public boolean canRaiseRequisition() {
        return admin || manager || recruiter;
    }
}
