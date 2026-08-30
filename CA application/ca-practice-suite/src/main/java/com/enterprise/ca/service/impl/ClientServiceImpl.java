package com.enterprise.ca.service.impl;

import com.enterprise.ca.dto.ClientDTO;
import com.enterprise.ca.dto.PageResponse;
import com.enterprise.ca.entity.Client;
import com.enterprise.ca.entity.ComplianceTask;
import com.enterprise.ca.exception.BusinessException;
import com.enterprise.ca.exception.DuplicateResourceException;
import com.enterprise.ca.exception.ResourceNotFoundException;
import com.enterprise.ca.mapper.ClientMapper;
import com.enterprise.ca.repository.ClientRepository;
import com.enterprise.ca.repository.ComplianceTaskRepository;
import com.enterprise.ca.repository.InvoiceRepository;
import com.enterprise.ca.repository.LedgerEntryRepository;
import com.enterprise.ca.repository.spec.ClientSpecifications;
import com.enterprise.ca.service.AuditService;
import com.enterprise.ca.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ComplianceTaskRepository complianceTaskRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientMapper clientMapper;
    private final AuditService auditService;

    @Override
    public ClientDTO create(ClientDTO dto) {
        validateUniqueIdentifiers(dto, null);
        Client saved = clientRepository.save(clientMapper.toEntity(dto));
        auditService.log("CREATE", "Client", saved.getId(), "Onboarded client: " + saved.getName());
        return clientMapper.toDTO(saved);
    }

    @Override
    public ClientDTO update(Long id, ClientDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        validateUniqueIdentifiers(dto, id);
        client.setName(dto.getName());
        client.setClientType(Client.ClientType.valueOf(dto.getClientType()));
        client.setGstin(blankToNull(dto.getGstin()));
        client.setPan(blankToNull(dto.getPan()));
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setAddressLine(dto.getAddressLine());
        client.setCity(dto.getCity());
        client.setState(dto.getState());
        client.setPincode(dto.getPincode());
        if (dto.getActive() != null) client.setActive(dto.getActive());
        Client updated = clientRepository.save(client);
        return enrich(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        return enrich(client);
    }

    @Override
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        client.setActive(false);
        clientRepository.save(client);
        auditService.log("DEACTIVATE", "Client", id, "Deactivated client: " + client.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientDTO> search(String name, String clientType, Boolean active, Pageable pageable) {
        Specification<Client> spec = Specification.where(ClientSpecifications.nameContains(name))
                .and(ClientSpecifications.typeEquals(clientType))
                .and(ClientSpecifications.activeEquals(active));
        Page<Client> page = clientRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::enrich));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> getAllActiveSimple() {
        return clientRepository.findAll(ClientSpecifications.activeEquals(true)).stream()
                .map(clientMapper::toDTO)
                .toList();
    }

    private ClientDTO enrich(Client client) {
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = LocalDate.now();
        BigDecimal income = ledgerEntryRepository.sumIncome(start, end);
        BigDecimal expense = ledgerEntryRepository.sumExpense(start, end);
        long pending = complianceTaskRepository.countByClientId(client.getId());
        return clientMapper.toDTO(client, income, expense, pending);
    }

    private void validateUniqueIdentifiers(ClientDTO dto, Long excludeId) {
        if (dto.getGstin() != null && !dto.getGstin().isBlank()
                && clientRepository.existsByGstinAndGstinIsNotNull(dto.getGstin())) {
            // Simple check - full "excludeId" dedupe is skipped for this MVP scope.
            if (excludeId == null) throw new DuplicateResourceException("A client with this GSTIN already exists");
        }
        if (dto.getPan() != null && !dto.getPan().isBlank()
                && clientRepository.existsByPanAndPanIsNotNull(dto.getPan())) {
            if (excludeId == null) throw new DuplicateResourceException("A client with this PAN already exists");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
