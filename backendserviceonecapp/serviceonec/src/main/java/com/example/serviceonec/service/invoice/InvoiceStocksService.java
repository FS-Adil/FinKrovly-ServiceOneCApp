package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;

import java.util.List;

public interface InvoiceStocksService {
    List<InvoiceStocksEntity> getAllInvoiceStocks();
}
