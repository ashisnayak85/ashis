package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // Resolves "which employee record does this logged-in username correspond to" -
    // the anchor for every self-service (dashboard/leaves/attendance) endpoint.
    Optional<Employee> findByUserUsername(String username);

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

    // --- Unpaged variants for "export ALL matching rows to Excel", not just the
    // current page - same filtering as the paginated finders above, just with a
    // Sort instead of a Pageable. Spring Data resolves these by parameter type,
    // so they can safely share a name with their Page-returning counterparts. ---

    List<Employee> findByActiveTrue(Sort sort);

    @Query("SELECT e FROM Employee e WHERE e.active = true AND " +
           "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Employee> searchEmployees(@Param("keyword") String keyword, Sort sort);

    @Query("SELECT e FROM Employee e WHERE " +
           "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Employee> searchAllEmployees(@Param("keyword") String keyword, Sort sort);

    long countByDepartmentId(Long departmentId);

    long countByDesignationId(Long designationId);

    long countByLocationId(Long locationId);

    // --- Resignation approval routing (ResignationServiceImpl.resolveApprover) ---

    // ROLE_IN_DEPARTMENT resolution: an active employee, linked to a user
    // holding the given role, within the given department. Ordered by id so
    // resolution is deterministic if more than one person qualifies.
    java.util.List<Employee> findByActiveTrueAndDepartmentIdAndUser_Roles_NameOrderByIdAsc(Long departmentId, String roleName);

    // SPECIFIC_ROLE resolution: same as above but department-agnostic
    // (e.g. a single company-wide HR Head role).
    java.util.List<Employee> findByActiveTrueAndUser_Roles_NameOrderByIdAsc(String roleName);

    // Hiring: all employee codes starting with a prefix - used by EmployeeCodeGenerator to find the next number.
    @Query("SELECT e.employeeCode FROM Employee e WHERE e.employeeCode LIKE CONCAT(:prefix, '%')")
    List<String> findEmployeeCodesByPrefix(@Param("prefix") String prefix);
}
