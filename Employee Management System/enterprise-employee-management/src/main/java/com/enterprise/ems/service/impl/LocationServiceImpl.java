package com.enterprise.ems.service.impl;

import com.enterprise.ems.dto.LocationDTO;
import com.enterprise.ems.entity.Location;
import com.enterprise.ems.exception.DuplicateResourceException;
import com.enterprise.ems.exception.ResourceNotFoundException;
import com.enterprise.ems.mapper.LocationMapper;
import com.enterprise.ems.repository.EmployeeRepository;
import com.enterprise.ems.repository.LocationRepository;
import com.enterprise.ems.service.AuditService;
import com.enterprise.ems.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final EmployeeRepository employeeRepository;
    private final LocationMapper locationMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "locations", allEntries = true)
    public LocationDTO create(LocationDTO dto) {
        if (locationRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Location name already exists");
        }
        if (locationRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Location code already exists");
        }
        Location saved = locationRepository.save(locationMapper.toEntity(dto));
        auditService.log("CREATE", "Location", saved.getId(), "Created location: " + saved.getName());
        return locationMapper.toDTO(saved, 0);
    }

    @Override
    @CacheEvict(value = "locations", allEntries = true)
    public LocationDTO update(Long id, LocationDTO dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
        locationMapper.updateEntity(location, dto);
        Location updated = locationRepository.save(location);
        long count = employeeRepository.countByLocationId(id);
        return locationMapper.toDTO(updated, count);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "locations", key = "#id")
    public LocationDTO getById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
        return locationMapper.toDTO(location, employeeRepository.countByLocationId(id));
    }

    @Override
    @CacheEvict(value = "locations", allEntries = true)
    public void delete(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
        location.setActive(false);
        locationRepository.save(location);
        auditService.log("DELETE", "Location", id, "Deactivated location: " + location.getName());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "locations", key = "'all'")
    public List<LocationDTO> getAllActive() {
        return locationRepository.findAll().stream()
                .filter(Location::getActive)
                .map(l -> locationMapper.toDTO(l, employeeRepository.countByLocationId(l.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "locations", key = "'allSimple'")
    public List<LocationDTO> getAllActiveSimple() {
        return locationRepository.findAll().stream()
                .filter(Location::getActive)
                .map(locationMapper::toDTOSimple)
                .toList();
    }
}
