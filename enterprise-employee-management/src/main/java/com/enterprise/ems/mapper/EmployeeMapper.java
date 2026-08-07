package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Converts between Entity and DTO layers
 * WHY: Keeps controllers/services free of mapping boilerplate
 */
@Component
public class EmployeeMapper {

    public EmployeeDTO toDTO(Employee entity) {
        if (entity == null) return null;
        return EmployeeDTO.builder()
                .id(entity.getId())
                .employeeCode(entity.getEmployeeCode())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .mobile(entity.getMobile())
                .dateOfBirth(entity.getDateOfBirth())
                .dateOfJoining(entity.getDateOfJoining())
                .salary(entity.getSalary())
                .designation(entity.getDesignation())
                .profilePhoto(entity.getProfilePhoto())
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .active(entity.getActive())
                .build();
    }

    public Employee toEntity(EmployeeDTO dto, Department department) {
        if (dto == null) return null;
        return Employee.builder()
                .id(dto.getId())
                .employeeCode(dto.getEmployeeCode())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .mobile(dto.getMobile())
                .dateOfBirth(dto.getDateOfBirth())
                .dateOfJoining(dto.getDateOfJoining())
                .salary(dto.getSalary())
                .designation(dto.getDesignation())
                .profilePhoto(dto.getProfilePhoto())
                .department(department)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public void updateEntity(Employee entity, EmployeeDTO dto, Department department) {
        entity.setEmployeeCode(dto.getEmployeeCode());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setMobile(dto.getMobile());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setDateOfJoining(dto.getDateOfJoining());
        entity.setSalary(dto.getSalary());
        entity.setDesignation(dto.getDesignation());
        entity.setDepartment(department);
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
