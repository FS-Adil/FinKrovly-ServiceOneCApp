package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.MeasurementUnitItemResponseDto;
import com.example.serviceonec.model.dto.response.MeasurementUnitResponseDto;
import com.example.serviceonec.model.dto.response.StructuralUnitItemResponseDto;
import com.example.serviceonec.model.dto.response.StructuralUnitResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeasurementUnitServiceImpl implements MeasurementUnitService {

    private final RestClientConfig restClientConfig;

    @Override
    public List<MeasurementUnitItemResponseDto> getAllMeasurementUnit() {

        log.info("--------> MeasurementUnitServiceImpl --------> getAllMeasurementUnit");

        MeasurementUnitResponseDto unitResponseDto = getResponse();

        List<MeasurementUnitItemResponseDto> list = new ArrayList<>(unitResponseDto.getValue());


        log.info("------> Все Единицы Измерения из 1с найдены и сохранены в базу");

        return list;
    }

    private MeasurementUnitResponseDto getResponse() {

        String url = "/Catalog_КлассификаторЕдиницИзмерения?" +
                "$select=Ref_Key,Description,НаименованиеПолное&" +
                "$format=json";

        MeasurementUnitResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(MeasurementUnitResponseDto.class);

        } catch (Exception e) {
            log.error(
                    String.format("Ошибка при получении Класификаторов Единиц Измерения за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        return response;
    }
}


