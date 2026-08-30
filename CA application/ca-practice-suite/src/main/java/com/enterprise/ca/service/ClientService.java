package com.enterprise.ca.service;

import com.enterprise.ca.dto.ClientDTO;
import com.enterprise.ca.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientDTO create(ClientDTO dto);
    ClientDTO update(Long id, ClientDTO dto);
    ClientDTO getById(Long id);
    void delete(Long id);
    PageResponse<ClientDTO> search(String name, String clientType, Boolean active, Pageable pageable);
    java.util.List<ClientDTO> getAllActiveSimple();
}
