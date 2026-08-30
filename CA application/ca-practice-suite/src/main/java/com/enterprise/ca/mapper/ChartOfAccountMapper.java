package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.ChartOfAccountDTO;
import com.enterprise.ca.entity.ChartOfAccount;
import org.springframework.stereotype.Component;

@Component
public class ChartOfAccountMapper {

    public ChartOfAccount toEntity(ChartOfAccountDTO dto) {
        return ChartOfAccount.builder()
                .name(dto.getName())
                .code(dto.getCode().toUpperCase())
                .accountType(ChartOfAccount.AccountType.valueOf(dto.getAccountType()))
                .active(dto.getActive() == null || dto.getActive())
                .build();
    }

    public ChartOfAccountDTO toDTO(ChartOfAccount a) {
        return ChartOfAccountDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .code(a.getCode())
                .accountType(a.getAccountType().name())
                .active(a.getActive())
                .build();
    }
}
