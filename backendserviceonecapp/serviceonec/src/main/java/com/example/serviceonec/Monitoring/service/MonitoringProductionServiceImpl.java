package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto;
import com.example.serviceonec.Monitoring.service.production.MonitoringProductionResultBuildData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringProductionServiceImpl implements MonitoringProductionService {

    private final MonitoringProductionResultBuildData monitoringProductionResultBuildData;

    @Override
    public Optional<MonitoringProductionInputResponseDto> getProductions() {
        return Optional.of(monitoringProductionResultBuildData.buildData());
    }
}
