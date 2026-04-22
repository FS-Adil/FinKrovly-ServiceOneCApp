package com.example.serviceonec.service;

import com.example.serviceonec.model.dto.response.MeasurementUnitItemResponseDto;

import java.util.List;

public interface MeasurementUnitService {
    List<MeasurementUnitItemResponseDto> getAllMeasurementUnit();
}
