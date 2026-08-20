package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringPmoInputResponseDto;
import com.example.serviceonec.Monitoring.service.pmo.MonitoringPmoResultBuildData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringPmoServiceImpl implements MonitoringPmoService {

    private final MonitoringPmoResultBuildData monitoringPmoResultBuildData;

    @Override
    public Optional<MonitoringPmoInputResponseDto> getAllPmo() {
        return Optional.of(monitoringPmoResultBuildData.buildData());
    }
}
