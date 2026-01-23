package com.example.serviceonec.service;

import com.example.serviceonec.model.entity.InvoiceEntity;

import java.util.List;

public interface InvoiceService {
//    InvoiceResponseDto getAllInvoice(LocalDateTime startDate, LocalDateTime endDate);
    List<InvoiceEntity> getAllInvoice();
}
