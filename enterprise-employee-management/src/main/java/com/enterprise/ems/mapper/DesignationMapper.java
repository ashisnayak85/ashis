package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

@Component
public class DesignationMapper {

    public DesignationDTO toDTO(Designation entity, long employeeCount) {
        if (entity == null) return null;
        return DesignationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .employeeCount(employeeCount)
                .build();
    }

    public DesignationDTO toDTOSimple(Designation entity) {
        if (entity == null) return null;
        return DesignationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }

    public Designation toEntity(DesignationDTO dto) {
        if (dto == null) return null;
        return Designation.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}
