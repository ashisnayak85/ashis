package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.entity.Designation;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.entity.Location;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.EmployeeMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.DesignationRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.LocationRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.EmployeeService;
import com.enterprise.ems.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * ================================================================================
 * PURPOSE: Employee Service Implementation - Business Logic Layer
 * ================================================================================
 * ANNOTATION: @Service - Registers as Spring Bean in IoC container
 * ANNOTATION: @Transactional - All methods run in a DB transaction
 *
 * FLOW: Controller -> Service (validation, business rules) -> Repository -> DB
 *
 * CACHING: @Cacheable on read, @CacheEvict on write (Phase 11)
 * ================================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAvailableForUserCreation() {
        return employeeRepository.findByActiveTrueAndUserIsNull().stream()
                .map(employeeMapper::toDTO)
                .toList();
    }

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeMapper employeeMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDTO create(EmployeeDTO dto) {
        log.info("Creating employee: {}", dto.getEmployeeCode());

        // Business validation: duplicate check
        if (employeeRepository.existsByEmployeeCode(dto.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee code already exists: " + dto.getEmployeeCode());
        }
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }
        if (dto.getAadharNumber() != null && !dto.getAadharNumber().isBlank()
                && employeeRepository.existsByAadharNumber(dto.getAadharNumber())) {
            throw new DuplicateResourceException("Aadhar number already registered to another employee");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));

        // Location is optional today (see EmployeeDTO.locationId comment) - resolve
        // only if the caller actually provided one.
        Location location = null;
        if (dto.getLocationId() != null) {
            location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + dto.getLocationId()));
        }

        // Designation is optional, same treatment as Location.
        Designation designation = null;
        if (dto.getDesignationId() != null) {
            designation = designationRepository.findById(dto.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + dto.getDesignationId()));
        }

        Employee employee = employeeMapper.toEntity(dto, department, location, designation);
        Employee saved = employeeRepository.save(employee);

        auditService.log("CREATE", "Employee", saved.getId(), "Created employee: " + saved.getEmployeeCode());
        log.debug("Employee created with ID: {}", saved.getId());

        return employeeMapper.toDTO(saved);
    }

    @Override
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDTO update(Long id, EmployeeDTO dto) {
        log.info("Updating employee ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        // Check duplicate email if changed
        if (!employee.getEmail().equals(dto.getEmail()) && employeeRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }

        // Check duplicate Aadhar if changed
        String newAadhar = dto.getAadharNumber();
        boolean aadharChanged = newAadhar != null && !newAadhar.isBlank()
                && !newAadhar.equals(employee.getAadharNumber());
        if (aadharChanged && employeeRepository.existsByAadharNumber(newAadhar)) {
            throw new DuplicateResourceException("Aadhar number already registered to another employee");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));

        Location location = null;
        if (dto.getLocationId() != null) {
            location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + dto.getLocationId()));
        }

        Designation designation = null;
        if (dto.getDesignationId() != null) {
            designation = designationRepository.findById(dto.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + dto.getDesignationId()));
        }

        employeeMapper.updateEntity(employee, dto, department, location, designation);
        Employee updated = employeeRepository.save(employee);

        auditService.log("UPDATE", "Employee", id, "Updated employee: " + updated.getEmployeeCode());
        return employeeMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "employees", key = "#id")
    public EmployeeDTO getById(Long id) {
        log.debug("Fetching employee by ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        return employeeMapper.toDTO(employee);
    }

    @Override
    @CacheEvict(value = "employees", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting employee ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        employee.setActive(false); // Soft delete - industry best practice
        employeeRepository.save(employee);
        auditService.log("DELETE", "Employee", id, "Soft-deleted employee: " + employee.getEmployeeCode());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> getAll(Pageable pageable) {
        Page<Employee> page = employeeRepository.findByActiveTrue(pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> search(String keyword, Pageable pageable) {
        Page<Employee> page = employeeRepository.searchEmployees(keyword, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> getAllEmployees(Pageable pageable) {
        // findAll(pageable) is inherited from JpaRepository -> "SELECT e FROM Employee e" (no active filter)
        Page<Employee> page = employeeRepository.findAll(pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> searchAllEmployees(String keyword, Pageable pageable) {
        Page<Employee> page = employeeRepository.searchAllEmployees(keyword, pageable);
        return toPageResponse(page);
    }

    // Same "active employees only" filter as getAll/search above, but the full
    // matching set (unpaged) rendered straight to .xlsx via the shared export util.
    @Override
    @Transactional(readOnly = true)
    public byte[] exportEmployees(String keyword) {
        Sort sort = Sort.by("firstName");
        List<Employee> employees = (keyword != null && !keyword.isBlank())
                ? employeeRepository.searchEmployees(keyword, sort)
                : employeeRepository.findByActiveTrue(sort);
        return toEmployeeXlsx("Employees", employees, true);
    }

    // Same "every row, active or not" filter as getAllEmployees/searchAllEmployees.
    @Override
    @Transactional(readOnly = true)
    public byte[] exportAllEmployees(String keyword) {
        Sort sort = Sort.by("firstName");
        List<Employee> employees = (keyword != null && !keyword.isBlank())
                ? employeeRepository.searchAllEmployees(keyword, sort)
                : employeeRepository.findAll(sort);
        return toEmployeeXlsx("All Employees", employees, false);
    }

    // includeLocation: EmployeeList shows a Location column, AllEmployeeList
    // doesn't - kept in sync with what each screen's table actually displays.
    private byte[] toEmployeeXlsx(String sheetName, List<Employee> employees, boolean includeLocation) {
        List<String> headers = includeLocation
                ? List.of("Code", "Name", "Email", "Department", "Location", "Designation", "Active")
                : List.of("Code", "Name", "Email", "Department", "Designation", "Status");

        return ExcelExportUtil.toXlsx(sheetName, headers, employees, emp -> includeLocation
                ? List.of(
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getEmail(),
                        emp.getDepartment() != null ? emp.getDepartment().getName() : "",
                        emp.getLocation() != null ? emp.getLocation().getName() : "",
                        emp.getDesignation() != null ? emp.getDesignation().getName() : "",
                        Boolean.TRUE.equals(emp.getActive()) ? "Yes" : "No")
                : List.of(
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getEmail(),
                        emp.getDepartment() != null ? emp.getDepartment().getName() : "",
                        emp.getDesignation() != null ? emp.getDesignation().getName() : "",
                        Boolean.TRUE.equals(emp.getActive()) ? "Active" : "Inactive"));
    }

    private PageResponse<EmployeeDTO> toPageResponse(Page<Employee> page) {
        return PageResponse.<EmployeeDTO>builder()
                .content(page.getContent().stream().map(employeeMapper::toDTO).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
