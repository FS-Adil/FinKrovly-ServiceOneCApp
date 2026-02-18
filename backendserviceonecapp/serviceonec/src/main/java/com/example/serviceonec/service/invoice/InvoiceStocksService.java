package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface InvoiceStocksService {
    Page<InvoiceStocksEntity> getAllInvoiceStocks();
    void getInvoiceStocksById(UUID uuid);
}
