package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CostPriceFullService {
    List<CostPriceControllerOutput> getAllCostPrice(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);
}
