package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringOrderInputResponseDto;
import com.example.serviceonec.Monitoring.service.order.MonitoringOrderResultBuildData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringOrderServiceImpl implements MonitoringOrderService{

    private final MonitoringOrderResultBuildData buildData;

    @Override
    public Optional<MonitoringOrderInputResponseDto> getOrders() {
        return Optional.of(buildData.buildTestData());
    }

}
