package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

/*
 * PURPOSE: Converts between Entity and DTO layers
 * WHY: Keeps controllers/services free of mapping boilerplate
 *
 * NOTE: EmployeeDTO keeps address/bank/statutory fields flat (e.g.
 * presentAddressLine, bankIfscCode) because that's simplest for the frontend
 * form and JSON payload. Employee entity groups the same fields into
 * @Embeddable objects (Address, BankDetails, StatutoryInfo) for cleaner JPA
 * mapping. This mapper is where that flat <-> grouped translation happens.
 */
@Component
public class EmployeeMapper {

    public EmployeeDTO toDTO(Employee entity) {
        if (entity == null) return null;

        Address present = entity.getPresentAddress();
        Address permanent = entity.getPermanentAddress();
        BankDetails bank = entity.getBankDetails();
        StatutoryInfo statutory = entity.getStatutoryInfo();

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
                .qualification(entity.getQualification())
                .yearOfPassing(entity.getYearOfPassing())
                .totalExperience(entity.getTotalExperience())
                .maritalStatus(entity.getMaritalStatus())
                .aadharNumber(entity.getAadharNumber())
                .salaryCalculationBasis(entity.getSalaryCalculationBasis())
                .presentAddressLine(present != null ? present.getAddressLine() : null)
                .presentCityDistrict(present != null ? present.getCityOrDistrict() : null)
                .presentState(present != null ? present.getState() : null)
                .presentPincode(present != null ? present.getPincode() : null)
                .permanentAddressLine(permanent != null ? permanent.getAddressLine() : null)
                .permanentCityDistrict(permanent != null ? permanent.getCityOrDistrict() : null)
                .permanentState(permanent != null ? permanent.getState() : null)
                .permanentPincode(permanent != null ? permanent.getPincode() : null)
                .bankName(bank != null ? bank.getBankName() : null)
                .bankAccountNumber(bank != null ? bank.getAccountNumber() : null)
                .bankIfscCode(bank != null ? bank.getIfscCode() : null)
                .pfApplicable(statutory != null ? statutory.getPfApplicable() : null)
                .pfNumber(statutory != null ? statutory.getPfNumber() : null)
                .uanNumber(statutory != null ? statutory.getUanNumber() : null)
                .restrictPf(statutory != null ? statutory.getRestrictPf() : null)
                .zeroPension(statutory != null ? statutory.getZeroPension() : null)
                .zeroPt(statutory != null ? statutory.getZeroPt() : null)
                .esiApplicable(statutory != null ? statutory.getEsiApplicable() : null)
                .esiNumber(statutory != null ? statutory.getEsiNumber() : null)
                .esiDispensation(statutory != null ? statutory.getEsiDispensation() : null)
                .qualificationCertificateFileId(entity.getQualificationCertificateFileId())
                .profilePhoto(entity.getProfilePhoto())
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .active(entity.getActive())
                .build();
    }

    public Employee toEntity(EmployeeDTO dto, Department department, Location location) {
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
                .qualification(dto.getQualification())
                .yearOfPassing(dto.getYearOfPassing())
                .totalExperience(dto.getTotalExperience())
                .maritalStatus(blankToNull(dto.getMaritalStatus()))
                .aadharNumber(blankToNull(dto.getAadharNumber()))
                .salaryCalculationBasis(blankToNull(dto.getSalaryCalculationBasis()))
                .presentAddress(buildAddress(dto.getPresentAddressLine(), dto.getPresentCityDistrict(),
                        dto.getPresentState(), dto.getPresentPincode()))
                .permanentAddress(buildAddress(dto.getPermanentAddressLine(), dto.getPermanentCityDistrict(),
                        dto.getPermanentState(), dto.getPermanentPincode()))
                .bankDetails(BankDetails.builder()
                        .bankName(dto.getBankName())
                        .accountNumber(dto.getBankAccountNumber())
                        .ifscCode(dto.getBankIfscCode())
                        .build())
                .statutoryInfo(StatutoryInfo.builder()
                        .pfApplicable(dto.getPfApplicable())
                        .pfNumber(dto.getPfNumber())
                        .uanNumber(dto.getUanNumber())
                        .restrictPf(dto.getRestrictPf())
                        .zeroPension(dto.getZeroPension())
                        .zeroPt(dto.getZeroPt())
                        .esiApplicable(dto.getEsiApplicable())
                        .esiNumber(dto.getEsiNumber())
                        .esiDispensation(dto.getEsiDispensation())
                        .build())
                .qualificationCertificateFileId(dto.getQualificationCertificateFileId())
                .profilePhoto(dto.getProfilePhoto())
                .department(department)
                .location(location)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public void updateEntity(Employee entity, EmployeeDTO dto, Department department, Location location) {
        entity.setEmployeeCode(dto.getEmployeeCode());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setMobile(dto.getMobile());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setDateOfJoining(dto.getDateOfJoining());
        entity.setSalary(dto.getSalary());
        entity.setDesignation(dto.getDesignation());
        entity.setQualification(dto.getQualification());
        entity.setYearOfPassing(dto.getYearOfPassing());
        entity.setTotalExperience(dto.getTotalExperience());
        entity.setMaritalStatus(blankToNull(dto.getMaritalStatus()));
        entity.setAadharNumber(blankToNull(dto.getAadharNumber()));
        entity.setSalaryCalculationBasis(blankToNull(dto.getSalaryCalculationBasis()));
        entity.setPresentAddress(buildAddress(dto.getPresentAddressLine(), dto.getPresentCityDistrict(),
                dto.getPresentState(), dto.getPresentPincode()));
        entity.setPermanentAddress(buildAddress(dto.getPermanentAddressLine(), dto.getPermanentCityDistrict(),
                dto.getPermanentState(), dto.getPermanentPincode()));
        entity.setBankDetails(BankDetails.builder()
                .bankName(dto.getBankName())
                .accountNumber(dto.getBankAccountNumber())
                .ifscCode(dto.getBankIfscCode())
                .build());
        entity.setStatutoryInfo(StatutoryInfo.builder()
                .pfApplicable(dto.getPfApplicable())
                .pfNumber(dto.getPfNumber())
                .uanNumber(dto.getUanNumber())
                .restrictPf(dto.getRestrictPf())
                .zeroPension(dto.getZeroPension())
                .zeroPt(dto.getZeroPt())
                .esiApplicable(dto.getEsiApplicable())
                .esiNumber(dto.getEsiNumber())
                .esiDispensation(dto.getEsiDispensation())
                .build());
        // Overwrite with whatever the frontend sent - update() payloads that
        // didn't touch the certificate still carry the old id (since getById()
        // populated the form with it), so this is safe either way.
        entity.setQualificationCertificateFileId(dto.getQualificationCertificateFileId());
        entity.setDepartment(department);
        entity.setLocation(location);
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }

    private Address buildAddress(String line, String cityDistrict, String state, String pincode) {
        return Address.builder()
                .addressLine(line)
                .cityOrDistrict(cityDistrict)
                .state(state)
                .pincode(pincode)
                .build();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
