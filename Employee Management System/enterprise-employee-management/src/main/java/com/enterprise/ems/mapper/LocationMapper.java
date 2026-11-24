package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationDTO toDTO(Location entity, long employeeCount) {
        if (entity == null) return null;
        return LocationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .address(entity.getAddress())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .pincode(entity.getPincode())
                .officeContact(entity.getOfficeContact())
                .timezone(entity.getTimezone())
                .active(entity.getActive())
                .employeeCount(employeeCount)
                .build();
    }

    // Lightweight version for pickers/dropdowns - skips the per-row employee
    // count query, same reasoning as DepartmentMapper.toDTOSimple.
    public LocationDTO toDTOSimple(Location entity) {
        if (entity == null) return null;
        return LocationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .city(entity.getCity())
                .state(entity.getState())
                .active(entity.getActive())
                .build();
    }

    public Location toEntity(LocationDTO dto) {
        if (dto == null) return null;
        return Location.builder()
                .id(dto.getId())
                .name(dto.getName())
                .code(dto.getCode())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .pincode(dto.getPincode())
                .officeContact(dto.getOfficeContact())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public void updateEntity(Location entity, LocationDTO dto) {
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry());
        entity.setPincode(dto.getPincode());
        entity.setOfficeContact(dto.getOfficeContact());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
