package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.example.serviceonec.model.dto.response.NomenclatureResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.mapper.NomenclatureMapper;
import com.example.serviceonec.repository.NomenclatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NomenclatureServiceImpl implements NomenclatureService {

    private final RestClientConfig restClientConfig;
    private final NomenclatureMapper nomenclatureMapper;
    private final NomenclatureRepository nomenclatureRepository;

    @Override
    public Page<NomenclatureEntity> getAllNomenclature() {

        log.info("-------> NomenclatureServiceImpl --------> getAllNomenclature");

        nomenclatureRepository.deleteAll();

        NomenclatureResponseDto nomenclatureResponseDto = getNomenclature();

        for (NomenclatureItemResponseDto value : nomenclatureResponseDto.getValue()) {
            nomenclatureRepository.save(nomenclatureMapper.toEntity(value));
        }


        log.info("------> Все Номенклатуры из 1с найдены и сохранены в базу");
        return nomenclatureRepository.findAll(PageRequest.of(0, 10));
    }

    private NomenclatureResponseDto getNomenclature() {
//        log.info("------> Старт метода по поиску в 1с всех Номенклатур");
        String url = "/Catalog_Номенклатура?" +
//                "$filter=DeletionMark eq false&" +
                "$select=Description,Code,Ref_Key&" +
                "$format=json";

        NomenclatureResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(NomenclatureResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Номенклатур за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

//        log.info("------> Конец метода по поиску в 1с всех Номенклатур");
        return response;
    }
}
