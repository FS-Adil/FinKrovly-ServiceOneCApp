package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {
//    InvoiceResponseDto getAllInvoice(LocalDateTime startDate, LocalDateTime endDate);
    Page<InvoiceEntity> getAllInvoice(UUID organizationId, LocalDateTime endDate);
}
