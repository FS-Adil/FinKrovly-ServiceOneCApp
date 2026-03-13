package com.example.serviceonec.controller.inventorycost;

import com.example.serviceonec.controller.assembly.input.AssemblyExpendControllerInput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.controller.inventorycost.output.InventoryCostControllerOutput;
import com.example.serviceonec.service.inventorycost.InventoryCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/inventory")
public class InventoryCostController {

    private final InventoryCostService inventoryCostService;

    @PostMapping("/balance-cost")
    public ResponseEntity<List<InventoryCostControllerOutput>> getAllInventory(
            @Valid @RequestBody AssemblyExpendControllerInput request
    ) {

        List<InventoryCostControllerOutput> list = inventoryCostService.getAllInventoryCost(
                request.getOrganizationId(),
                request.getDateTo()
        );

        if (list.isEmpty()) {
            log.error("Нет данных Организаций по данному UUID - {}", request.getOrganizationId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Нет данных Организаций по данному UUID - %s", request.getOrganizationId().toString())
            );
        }

        return ResponseEntity.ok(list);
    }
}
