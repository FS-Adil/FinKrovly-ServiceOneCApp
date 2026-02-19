package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;

import java.util.List;
import java.util.UUID;

public interface CostPriceService {
    List<CostPriceControllerOutput> getAllCostPrice(UUID organizationId);
}
