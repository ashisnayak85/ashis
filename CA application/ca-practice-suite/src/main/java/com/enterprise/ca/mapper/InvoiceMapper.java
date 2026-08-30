package com.enterprise.ca.mapper;

import com.enterprise.ca.dto.InvoiceDTO;
import com.enterprise.ca.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceDTO toDTO(Invoice i) {
        return InvoiceDTO.builder()
                .id(i.getId())
                .invoiceNumber(i.getInvoiceNumber())
                .clientId(i.getClient().getId())
                .clientName(i.getClient().getName())
                .invoiceType(i.getInvoiceType().name())
                .invoiceDate(i.getInvoiceDate())
                .dueDate(i.getDueDate())
                .description(i.getDescription())
                .subtotal(i.getSubtotal())
                .gstRate(i.getGstRate())
                .gstAmount(i.getGstAmount())
                .totalAmount(i.getTotalAmount())
                .status(i.getStatus().name())
                .build();
    }
}
