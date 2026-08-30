package com.enterprise.ca.service;

import com.enterprise.ca.dto.InvoiceDTO;
import com.enterprise.ca.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    InvoiceDTO create(InvoiceDTO dto);
    InvoiceDTO updateStatus(Long id, String status, String actor);
    InvoiceDTO getById(Long id);
    void delete(Long id);
    PageResponse<InvoiceDTO> search(Long clientId, String type, String status, Pageable pageable);
}
