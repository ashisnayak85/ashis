package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.EmployeeTypeDTO;
import com.enterprise.ems.entity.EmployeeType;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.EmployeeTypeMapper;
import com.enterprise.ems.repository.EmployeeTypeRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.DepartmentService;
import com.enterprise.ems.service.EmployeeTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeTypeServiceImpl implements EmployeeTypeService{


    private final EmployeeTypeRepository employeeTypeRepository;
    private final EmployeeTypeMapper employeeTypeMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "employee_type", allEntries = true)
    public EmployeeTypeDTO create(EmployeeTypeDTO dto) {
        if (employeeTypeRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Employee Type name already exists");
        }
        if (employeeTypeRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Employee Type code already exists");
        }
        EmployeeType toSave = employeeTypeMapper.toEntity(dto);
        EmployeeType saved = employeeTypeRepository.save(toSave);
        auditService.log("CREATE", "Employee Type", saved.getId(), "Created Employee Type: " + saved.getName());
        return employeeTypeMapper.toDTO(saved);
    }

    @Override
    @CacheEvict(value = "employee_type", allEntries = true)
    public EmployeeTypeDTO update(Long id, EmployeeTypeDTO dto) {
    	EmployeeType dept = employeeTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Type not found: " + id));
        dept.setName(dto.getName());
        dept.setCode(dto.getCode());
        dept.setDescription(dto.getDescription());
        if (dto.getActive() != null) dept.setActive(dto.getActive());
        EmployeeType updated = employeeTypeRepository.save(dept);
        return employeeTypeMapper.toDTO(updated);
    }

//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "employee_type", key = "#id")
//    public EmployeeTypeDTO getById(Long id) {
//    	EmployeeType dept = employeeTypeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee Type not found: " + id));
//        return employeeTypeMapper.toDTO(dept, employeeRepository.countByDepartmentId(id));
//    }

    @Override
    @CacheEvict(value = "employee_type", allEntries = true)
    public void delete(Long id) {
    	EmployeeType dept = employeeTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Type not found: " + id));
        dept.setActive(false);
        employeeTypeRepository.save(dept);
        auditService.log("DELETE", "Employee Type", id, "Deactivated Employee Type: " + dept.getName());
    }

//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "employee_type", key = "'all'")
//    public List<EmployeeTypeDTO> getAllActive() {
//        return employeeTypeRepository.findAll().stream()
//                .map(employeeTypeMapper::toDTO)
//                .toList();
//    }
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "employee_type", key = "'all'")
    public List<EmployeeTypeDTO> getAllActive() {
        return employeeTypeRepository.findByActiveTrue().stream()
                .map(employeeTypeMapper::toDTO)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "employee_type", key = "'allSimple'")
    public List<EmployeeTypeDTO> getAllActiveSimple() {
        return employeeTypeRepository.findAll().stream()
                .filter(EmployeeType::getActive)
                .map(employeeTypeMapper::toDTOSimple)
                .toList();
    }

}
