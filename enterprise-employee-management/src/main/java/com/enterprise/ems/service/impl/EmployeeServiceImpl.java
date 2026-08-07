package com.enterprise.ems.service.impl;

import com.enterprise.ems.constant.AppConstants;
import com.enterprise.ems.dto.EmployeeDTO;
import com.enterprise.ems.dto.PageResponse;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.entity.Employee;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.EmployeeMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
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

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));

        Employee employee = employeeMapper.toEntity(dto, department);
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

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));

        employeeMapper.updateEntity(employee, dto, department);
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
