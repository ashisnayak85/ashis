package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.ClientDTO;
import com.enterprise.ca.entity.Client;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class ClientMapper {

    public Client toEntity(ClientDTO dto) {
        return Client.builder()
                .name(dto.getName())
                .clientType(Client.ClientType.valueOf(dto.getClientType()))
                .gstin(blankToNull(dto.getGstin()))
                .pan(blankToNull(dto.getPan()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .addressLine(dto.getAddressLine())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .active(dto.getActive() == null || dto.getActive())
                .build();
    }

    public ClientDTO toDTO(Client c) {
        return toDTO(c, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    }

    public ClientDTO toDTO(Client c, BigDecimal totalIncome, BigDecimal totalExpense, Long pendingCompliance) {
        return ClientDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .clientType(c.getClientType().name())
                .gstin(c.getGstin())
                .pan(c.getPan())
                .email(c.getEmail())
                .phone(c.getPhone())
                .addressLine(c.getAddressLine())
                .city(c.getCity())
                .state(c.getState())
                .pincode(c.getPincode())
                .active(c.getActive())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .pendingComplianceCount(pendingCompliance)
                .build();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
