package com.example.serviceonec.service.inventory;

import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import com.example.serviceonec.model.entity.inventory.InventoryStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.springframework.data.domain.Page;

public interface InventoryService {
    Page<InventoryEntity> getAllInventory();
    Page<InventoryStocksEntity> getAllInventoryStocks();
}
