package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto;

import java.util.Optional;

public interface MonitoringProductionService {
    Optional<MonitoringProductionInputResponseDto> getProductions();
}
