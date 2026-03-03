package com.example.serviceonec.service.expend;

import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExpendStocksService {
    Page<ExpendStocksEntity> getAllExpendStocks();
    void findExpendStocksById(UUID id);
    Map<UUID, List<ExpendStocksEntity>> findExpendStocksByIds(List<UUID> ids);
}
