package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.CharacteristicItemResponseDto;
import com.example.serviceonec.model.dto.response.CharacteristicResponseDto;
import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.example.serviceonec.model.dto.response.NomenclatureResponseDto;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.mapper.CharacteristicMapper;
import com.example.serviceonec.repository.CharacteristicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CharacteristicServiceImpl implements CharacteristicService{

    private final RestClientConfig restClientConfig;
    private final CharacteristicMapper characteristicMapper;
    private final CharacteristicRepository characteristicRepository;

    @Override
    public Page<CharacteristicEntity> getAllCharacteristic() {

        characteristicRepository.deleteAll();

        CharacteristicResponseDto characteristicResponseDto = getCharacteristic();

        for (CharacteristicItemResponseDto value : characteristicResponseDto.getValue()) {
            characteristicRepository.save(characteristicMapper.toEntity(value));
        }


        log.info("------> Все Характеристики из 1с найдены и сохранены в базу");
        return characteristicRepository.findAll(PageRequest.of(0, 10));
    }

    private CharacteristicResponseDto getCharacteristic() {
        log.info("------> Старт метода по поиску в 1с всех Характеристик");
        String url = "/Catalog_ХарактеристикиНоменклатуры?" +
//                "$filter=DeletionMark eq false&" +
                "$select=Description,Code,Ref_Key&" +
                "$format=json";

        CharacteristicResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(CharacteristicResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Характеристик за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех Характеристик");
        return response;
    }
}
