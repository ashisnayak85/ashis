package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.DepartmentDTO;
import com.enterprise.ems.entity.Department;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.DepartmentMapper;
import com.enterprise.ems.repository.DepartmentRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDTO create(DepartmentDTO dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department name already exists");
        }
        if (departmentRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Department code already exists");
        }
        Department saved = departmentRepository.save(departmentMapper.toEntity(dto));
        auditService.log("CREATE", "Department", saved.getId(), "Created department: " + saved.getName());
        return departmentMapper.toDTO(saved, 0);
    }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        dept.setName(dto.getName());
        dept.setCode(dto.getCode());
        dept.setDescription(dto.getDescription());
        if (dto.getActive() != null) dept.setActive(dto.getActive());
        Department updated = departmentRepository.save(dept);
        long count = employeeRepository.countByDepartmentId(id);
        return departmentMapper.toDTO(updated, count);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "#id")
    public DepartmentDTO getById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        return departmentMapper.toDTO(dept, employeeRepository.countByDepartmentId(id));
    }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public void delete(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        dept.setActive(false);
        departmentRepository.save(dept);
        auditService.log("DELETE", "Department", id, "Deactivated department: " + dept.getName());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "'all'")
    public List<DepartmentDTO> getAllActive() {
        return departmentRepository.findAll().stream()
                .filter(Department::getActive)
                .map(d -> departmentMapper.toDTO(d, employeeRepository.countByDepartmentId(d.getId())))
                .toList();
    }
}
