package com.example.serviceonec.service.invoice;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface InvoiceStocksService {
    Page<InvoiceStocksEntity> getAllInvoiceStocks();
    void getInvoiceStocksById(UUID uuid);
    List<InvoiceStocksEntity> findInvoiceStocksByIds(Set<UUID> ids);
}
