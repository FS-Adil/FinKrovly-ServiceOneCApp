package com.example.serviceonec.controller.inventory;

import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import com.example.serviceonec.model.entity.inventory.InventoryStocksEntity;
import com.example.serviceonec.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryStocksController {

    private final InventoryService inventoryService;

    @GetMapping("/get-all-inventory-stocks")
    public ResponseEntity<Page<InventoryStocksEntity>> getAllInventoryStocks() {

        Page<InventoryStocksEntity> inventoryStocksEntities = inventoryService.getAllInventoryStocks();

        if (inventoryStocksEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(inventoryStocksEntities);
    }
}
