package com.example.serviceonec.Monitoring.controller;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.dto.input.MonitoringPmoInputResponseDto;
import com.example.serviceonec.Monitoring.service.MonitoringPmoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/section/pmo")
@RequiredArgsConstructor
@Slf4j
public class MonitoringPmoController {

    private final MonitoringPmoService monitoringPmoService;

    @GetMapping
    public ResponseEntity<MonitoringPmoInputResponseDto> getOrders() {

        Optional<MonitoringPmoInputResponseDto> orders = monitoringPmoService.getAllPmo();

        if (orders.isPresent()) {
            log.info("MonitoringPmoController -> getOrders -> ПМО успешно найдены");
            return ResponseEntity.ok(orders.get());
        } else {
            log.warn("MonitoringPmoController -> getOrders -> ПМО отсутствуют");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
