package com.enterprise.ems.mapper;
import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTypeMapper {


    public EmployeeTypeDTO toDTO(EmployeeType entity) {
        if (entity == null) return null;
        return EmployeeTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }
    public EmployeeTypeDTO toDTOSimple(EmployeeType entity) {
        if (entity == null) return null;
        return EmployeeTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }

    public EmployeeType toEntity(EmployeeTypeDTO dto) {
        if (dto == null) return null;
        return EmployeeType.builder()
                .id(dto.getId())
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

}
