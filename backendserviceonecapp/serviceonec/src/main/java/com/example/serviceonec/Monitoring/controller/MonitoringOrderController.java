package com.example.serviceonec.Monitoring.controller;

import com.example.serviceonec.Mobile.dto.input.RollMobileInputResponseDto;
import com.example.serviceonec.Mobile.service.RollMobileService;
import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.service.MonitoringOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/section/customer-order")
@RequiredArgsConstructor
@Slf4j
public class MonitoringOrderController {

    private final MonitoringOrderService monitoringOrderService;

    @GetMapping
    public ResponseEntity<MonitoringOrderInputResponseDto> getOrders() {

        Optional<MonitoringOrderInputResponseDto> orders = monitoringOrderService.getOrders();

        if (orders.isPresent()) {
            log.info("MonitoringOrderController -> getOrders -> Заказы успешно найдены");
            return ResponseEntity.ok(orders.get());
        } else {
            log.warn("MonitoringOrderController -> getOrders -> Заказы отсутствуют");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
