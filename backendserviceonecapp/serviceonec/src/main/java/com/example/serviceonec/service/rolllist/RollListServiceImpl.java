package com.example.serviceonec.service.rolllist;

import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListResponseDto;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.rolllist.RollListEntity;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RollListServiceImpl implements RollListService {

    private final RestClientConfig restClientConfig;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;

    private final OneCProperties oneCProperties;

    private final BatchService batchService;

    @Override
    public List<RollListEntity> getAllClosedRoll() {

        log.info("-->RollListService-->getAllClosedRoll--вызов метода");

        List<RollListEntity> list = new ArrayList<>();

        Map<String, String> oneCGuid = new HashMap<>();
        oneCGuid.put(oneCProperties.getOneCGuid(), "Под краном");
        oneCGuid.put(oneCProperties.getOneCGuidOpen(), "В цеху");
        oneCGuid.put(oneCProperties.getOneCGuidDefective(), "БРАК");

        for (Map.Entry<String, String> guid : oneCGuid.entrySet()) {

            RollListResponseDto rollListResponseDto = getResponse(guid.getKey(), guid.getValue());

            for (RollListItemResponseDto item : rollListResponseDto.getValue()) {
                if (item.getBatchKey().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000000")) == 0) {
                    continue;
                }
                if (item.getQuantityBalance().compareTo(0.0) > 0) {
                    String nomenclatureName = null;
                    String characteristicName = null;
                    String batchName = null;
                    BigDecimal batchWeight = null;
                    BigDecimal batchLength = null;

                    try {
                        NomenclatureEntity nomenclature = nomenclatureRepository.findByRefKey(item.getNomenclatureKey());
                        if (nomenclature != null) {
                            nomenclatureName = nomenclature.getDescription();
                        } else log.debug("Номенкулатура {} не найдена", item.getNomenclatureKey());
                    } catch (Exception e) {
                        // Логирование или обработка ошибки
                        log.error("Ошибка при получении номенклатуры: {}", e.getMessage());
                        nomenclatureName = "Не найдено";
                    }

                    try {
                        CharacteristicEntity characteristic = characteristicRepository.findByRefKey(item.getCharacteristicKey());
                        if (characteristic != null) {
                            characteristicName = characteristic.getDescription();
                        } else log.debug("Характеристика {} не найдена", item.getCharacteristicKey());
                    } catch (Exception e) {
                        log.error("Ошибка при получении характеристики: {}", e.getMessage());
                        characteristicName = "Не найдено";
                    }

                    BatchEntity batch = null;

                    try {
                        batch = batchRepository.findByRefKey(item.getBatchKey());

                        if (batch == null) {
                            log.debug("Партия {} не найдена", item.getBatchKey());
                            log.info("--> Запускаем сервис по обновлению репозиторий по партиям");
                            batchService.getAllBatch();

                            batch = batchRepository.findByRefKey(item.getBatchKey());
                        }

                        if (batch != null) {
                            batchName = batch.getDescription();
                        } else {
                            batchName = "Не найдено";
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при получении партии: {}", e.getMessage());
                        batchName = "Не найдено";
                    }

                    if (batch != null) {
                        batchWeight = batch.getWeight();
                    } else {
                        batchWeight = BigDecimal.valueOf(0.00);
                    }

                    if (batch != null) {
                        batchLength = batch.getLength();
                    } else {
                        batchLength = BigDecimal.valueOf(0.00);
                    }

                    list.add(
                            RollListEntity.builder()
                                    .nomenclatureName(nomenclatureName)
                                    .characteristicName(characteristicName)
                                    .batchName(batchName)
                                    .quantityBalance(item.getQuantityBalance())
                                    .weight(batchWeight)
                                    .length(batchLength)
                                    .location(guid.getValue())
                                    .build()
                    );
                }
            }
        }

        return list;
    }

    private RollListResponseDto getResponse(String key, String value) {

        log.info("------> Старт метода по поиску в 1с всех Рулонов {}", value);

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Condition='cast(СтруктурнаяЕдиница, 'Catalog_СтруктурныеЕдиницы') eq guid'%s'')" +
                "?" +
                "$select=Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance, СуммаBalance&" +
                "$format=json", key);

        RollListResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(RollListResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Списка рулонов за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех Рулонов {}", value);

        return response;
    }
}


