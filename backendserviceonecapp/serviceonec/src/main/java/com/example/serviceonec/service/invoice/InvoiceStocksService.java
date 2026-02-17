package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;

import java.util.List;
import java.util.UUID;

public interface InvoiceStocksService {
    List<InvoiceStocksEntity> getAllInvoiceStocks();
    void getInvoiceStocksById(UUID uuid);
}
