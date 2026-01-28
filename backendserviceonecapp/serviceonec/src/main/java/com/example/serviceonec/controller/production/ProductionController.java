package com.example.serviceonec.controller.production;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.service.production.ProductionService;
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
@RequestMapping("/api/v1/production")
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping("/save-all-production")
    public ResponseEntity<Page<ProductionEntity>> getAllProduction() {
        LocalDateTime startDate = LocalDateTime.of(2025, 12, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

//        Optional<InvoiceResponseDto> invoiceResponseDtoOptional = Optional.ofNullable(invoiceService.getAllInvoice(startDate, endDate));
        Page<ProductionEntity> productionEntities = productionService.getAllProduction(startDate, endDate);

        if (productionEntities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
        return ResponseEntity.ok(productionEntities);
    }

}
