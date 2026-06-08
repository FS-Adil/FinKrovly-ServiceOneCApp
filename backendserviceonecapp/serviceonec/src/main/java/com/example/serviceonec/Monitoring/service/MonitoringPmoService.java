package com.example.serviceonec.Monitoring.service;

import com.example.serviceonec.Monitoring.dto.input.MonitoringPmoInputResponseDto;

import java.util.Optional;

public interface MonitoringPmoService {
    Optional<MonitoringPmoInputResponseDto> getAllPmo();
}
