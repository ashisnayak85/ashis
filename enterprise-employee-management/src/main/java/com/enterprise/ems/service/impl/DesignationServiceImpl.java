package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.DesignationDTO;
import com.enterprise.ems.entity.Designation;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.DesignationMapper;
import com.enterprise.ems.repository.DesignationRepository;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.DesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final DesignationMapper designationMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "designations", allEntries = true)
    public DesignationDTO create(DesignationDTO dto) {
        if (designationRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Designation name already exists");
        }
        Designation saved = designationRepository.save(designationMapper.toEntity(dto));
        auditService.log("CREATE", "Designation", saved.getId(), "Created designation: " + saved.getName());
        return designationMapper.toDTO(saved, 0);
    }

    @Override
    @CacheEvict(value = "designations", allEntries = true)
    public DesignationDTO update(Long id, DesignationDTO dto) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + id));
        if (!designation.getName().equals(dto.getName()) && designationRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Designation name already exists");
        }
        designation.setName(dto.getName());
        designation.setDescription(dto.getDescription());
        if (dto.getActive() != null) designation.setActive(dto.getActive());
        Designation updated = designationRepository.save(designation);
        long count = employeeRepository.countByDesignationId(id);
        return designationMapper.toDTO(updated, count);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "designations", key = "#id")
    public DesignationDTO getById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + id));
        return designationMapper.toDTO(designation, employeeRepository.countByDesignationId(id));
    }

    @Override
    @CacheEvict(value = "designations", allEntries = true)
    public void delete(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + id));
        designation.setActive(false);
        designationRepository.save(designation);
        auditService.log("DELETE", "Designation", id, "Deactivated designation: " + designation.getName());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "designations", key = "'all'")
    public List<DesignationDTO> getAllActive() {
        return designationRepository.findAll().stream()
                .filter(Designation::getActive)
                .map(d -> designationMapper.toDTO(d, employeeRepository.countByDesignationId(d.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "designations", key = "'allSimple'")
    public List<DesignationDTO> getAllActiveSimple() {
        return designationRepository.findAll().stream()
                .filter(Designation::getActive)
                .map(designationMapper::toDTOSimple)
                .toList();
    }
}
