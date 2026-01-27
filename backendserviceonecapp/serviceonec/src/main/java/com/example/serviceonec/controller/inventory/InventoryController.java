package com.example.serviceonec.controller.inventory;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.inventory.InventoryEntity;
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
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/save-all-inventory")
    public ResponseEntity<Page<InventoryEntity>> getAllInvoice() {
        LocalDateTime startDate = LocalDateTime.of(2025, 12, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        Page<InventoryEntity> inventoryEntities = inventoryService.getAllInventory();

        if (inventoryEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(inventoryEntities);
    }

}
