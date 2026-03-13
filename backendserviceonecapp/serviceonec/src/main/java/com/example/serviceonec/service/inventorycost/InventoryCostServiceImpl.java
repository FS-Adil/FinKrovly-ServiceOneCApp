package com.example.serviceonec.service.inventorycost;

import com.example.serviceonec.controller.inventorycost.output.InventoryCostControllerOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryCostServiceImpl implements InventoryCostService{
    @Override
    public List<InventoryCostControllerOutput> getAllInventoryCost(UUID organizationId, LocalDateTime endDate) {
        return List.of(InventoryCostControllerOutput.builder()
                        .refKey(UUID.randomUUID().toString())
                        .name("Ntcndkas")
                        .characteristic("afasfdasfas")
                        .batch("sdlfjasfkas;jf")
                        .cost(BigDecimal.ZERO)
                        .quantity(BigDecimal.ONE)
                .build());
    }
}
