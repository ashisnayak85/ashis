package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Active employees with no linked User account - powers the "create user" picker
    List<Employee> findByActiveTrueAndUserIsNull();

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByAadharNumber(String aadharNumber);

    Page<Employee> findByActiveTrue(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.active = true AND " +
           "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> searchEmployees(@Param("keyword") String keyword, Pageable pageable);

    // SELECT * FROM Employee (no active filter) - used by the "All Employees" screen
    @Query("SELECT e FROM Employee e WHERE " +
           "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> searchAllEmployees(@Param("keyword") String keyword, Pageable pageable);

    long countByDepartmentId(Long departmentId);

    long countByLocationId(Long locationId);
}
