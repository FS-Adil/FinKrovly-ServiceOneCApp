package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;

import java.util.Optional;

public interface MonitoringOrderService {
    Optional<MonitoringOrderInputResponseDto> getOrders();
}
