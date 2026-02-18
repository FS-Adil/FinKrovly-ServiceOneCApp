package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.StructuralUnitItemResponseDto;
import com.example.serviceonec.model.dto.response.StructuralUnitResponseDto;
import com.example.serviceonec.model.entity.StructuralUnitEntity;
import com.example.serviceonec.model.mapper.StructuralUnitMapper;
import com.example.serviceonec.repository.StructuralUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StructuralUnitServiceImpl implements StructuralUnitService{

    private final RestClientConfig restClientConfig;
    private final StructuralUnitMapper structuralUnitMapper;
    private final StructuralUnitRepository structuralUnitRepository;

    @Override
    public Page<StructuralUnitEntity> getAllStructuralUnit() {

        log.info("--------> StructuralUnitServiceImpl --------> getAllStructuralUnit");

        structuralUnitRepository.deleteAll();

        StructuralUnitResponseDto structuralUnitResponseDto = gerResponse();

        for (StructuralUnitItemResponseDto value : structuralUnitResponseDto.getValue()) {
            structuralUnitRepository.save(structuralUnitMapper.toEntity(value));
        }


        log.info("------> Все Структурные Еденицы из 1с найдены и сохранены в базу");

        return structuralUnitRepository.findAll(PageRequest.of(0, 10));
    }

    private StructuralUnitResponseDto gerResponse() {

//        log.info("------> Старт метода по поиску в 1с всех Структурных Едениц");
        String url = "/Catalog_СтруктурныеЕдиницы?" +
                "$select=Ref_Key,Code,Description,ТипСтруктурнойЕдиницы,Организация_Key&" +
                "$format=json";

        StructuralUnitResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(StructuralUnitResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Структурных Едениц за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

//        log.info("------> Конец метода по поиску в 1с всех Структурных Едениц");

        return response;
    }
}
