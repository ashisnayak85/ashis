package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.ChartOfAccountDTO;
import com.enterprise.ca.entity.ChartOfAccount;
import com.enterprise.ca.exception.DuplicateResourceException;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.ChartOfAccountMapper;
import com.enterprise.ca.repository.ChartOfAccountRepository;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.ChartOfAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChartOfAccountServiceImpl implements ChartOfAccountService {

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ChartOfAccountMapper chartOfAccountMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "accounts", allEntries = true)
    public ChartOfAccountDTO create(ChartOfAccountDTO dto) {
        if (chartOfAccountRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("An account with this name already exists");
        }
        if (chartOfAccountRepository.existsByCode(dto.getCode().toUpperCase())) {
            throw new DuplicateResourceException("An account with this code already exists");
        }
        ChartOfAccount saved = chartOfAccountRepository.save(chartOfAccountMapper.toEntity(dto));
        auditService.log("CREATE", "ChartOfAccount", saved.getId(), "Created ledger head: " + saved.getName());
        return chartOfAccountMapper.toDTO(saved);
    }

    @Override
    @CacheEvict(value = "accounts", allEntries = true)
    public ChartOfAccountDTO update(Long id, ChartOfAccountDTO dto) {
        ChartOfAccount account = chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger head not found: " + id));
        account.setName(dto.getName());
        account.setAccountType(ChartOfAccount.AccountType.valueOf(dto.getAccountType()));
        if (dto.getActive() != null) account.setActive(dto.getActive());
        return chartOfAccountMapper.toDTO(chartOfAccountRepository.save(account));
    }

    @Override
    @CacheEvict(value = "accounts", allEntries = true)
    public void delete(Long id) {
        ChartOfAccount account = chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger head not found: " + id));
        account.setActive(false);
        chartOfAccountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "'active'")
    public List<ChartOfAccountDTO> getAllActive() {
        return chartOfAccountRepository.findAll().stream()
                .filter(ChartOfAccount::getActive)
                .map(chartOfAccountMapper::toDTO)
                .toList();
    }
}
