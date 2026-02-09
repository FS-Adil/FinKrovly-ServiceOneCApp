package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceEntity;

import java.util.List;

public interface InvoiceService {
//    InvoiceResponseDto getAllInvoice(LocalDateTime startDate, LocalDateTime endDate);
    List<InvoiceEntity> getAllInvoice();
}
