package com.example.serviceonec.Monitoring.controller;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto;
import com.example.serviceonec.Monitoring.service.MonitoringProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/section/production")
@RequiredArgsConstructor
@Slf4j
public class MonitoringProductionController {

    private final MonitoringProductionService monitoringProductionService;

    @GetMapping
    public ResponseEntity<MonitoringProductionInputResponseDto> getOrders() {

        Optional<MonitoringProductionInputResponseDto> orders = monitoringProductionService.getProductions();

        if (orders.isPresent()) {
            log.info("MonitoringProductionController -> getOrders -> Производства успешно найдены");
            return ResponseEntity.ok(orders.get());
        } else {
            log.warn("MonitoringProductionController -> getOrders -> Производства отсутствуют");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
