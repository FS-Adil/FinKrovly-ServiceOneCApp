package com.example.serviceonec.service.inventorycost;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.controller.inventorycost.output.InventoryCostControllerOutput;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InventoryCostService {
    List<InventoryCostControllerOutput> getAllInventoryCost(UUID organizationId, LocalDateTime endDate);
}
