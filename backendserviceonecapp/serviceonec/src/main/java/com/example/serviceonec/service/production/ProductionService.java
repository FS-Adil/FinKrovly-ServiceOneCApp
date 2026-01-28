package com.example.serviceonec.service.production;

import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import com.example.serviceonec.model.entity.production.ProductionStocksEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface ProductionService {
    Page<ProductionEntity> getAllProduction(LocalDateTime startDate, LocalDateTime endDate);
    Page<ProductionItemsEntity> getAllProductionItems();
    Page<ProductionStocksEntity> getAllProductionStocks();
}
