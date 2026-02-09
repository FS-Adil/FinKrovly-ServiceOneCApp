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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RollListServiceImpl implements RollListService {

    private final RestClientConfig restClientConfig;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;

    private final OneCProperties oneCProperties;

    @Override
    public List<RollListEntity> getAllClosedRoll() {

        log.info("-->RollListService-->getAllClosedRoll--вызов метода");

        List<RollListEntity> list = new ArrayList<>();

        RollListResponseDto rollListResponseDto = getResponse();

//        for (RollListItemResponseDto item : rollListResponseDto.getValue()) {
//
//            if (item.getQuantityBalance().compareTo(0.0) > 0) {
//                list.add(
//                        RollListEntity.builder()
//                                .nomenclatureName(nomenclatureRepository.findByRefKey(item.getNomenclatureKey())
//                                        .getDescription())
//                                .characteristicName(characteristicRepository.findByRefKey(item.getCharacteristicKey())
//                                        .getDescription())
//                                .batchName(batchRepository.findByRefKey(item.getBatchKey())
//                                        .getDescription())
//                                .quantityBalance(item.getQuantityBalance())
//                                .amountBalance(item.getAmountBalance())
//                                .build()
//                );
//            }
//        }

        for (RollListItemResponseDto item : rollListResponseDto.getValue()) {
            if (item.getQuantityBalance().compareTo(0.0) > 0) {
                String nomenclatureName = null;
                String characteristicName = null;
                String batchName = null;

                try {
                    NomenclatureEntity nomenclature = nomenclatureRepository.findByRefKey(item.getNomenclatureKey());
                    if (nomenclature != null) {
                        nomenclatureName = nomenclature.getDescription();
                    }
                } catch (Exception e) {
                    // Логирование или обработка ошибки
                    System.err.println("Ошибка при получении номенклатуры: " + e.getMessage());
                    nomenclatureName = "Не найдено";
                }

                try {
                    CharacteristicEntity characteristic = characteristicRepository.findByRefKey(item.getCharacteristicKey());
                    if (characteristic != null) {
                        characteristicName = characteristic.getDescription();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при получении характеристики: " + e.getMessage());
                    characteristicName = "Не найдено";
                }

                try {
                    BatchEntity batch = batchRepository.findByRefKey(item.getBatchKey());
                    if (batch != null) {
                        batchName = batch.getDescription();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при получении партии: " + e.getMessage());
                    batchName = "Не найдено";
                }

                list.add(
                        RollListEntity.builder()
                                .nomenclatureName(nomenclatureName)
                                .characteristicName(characteristicName)
                                .batchName(batchName)
                                .quantityBalance(item.getQuantityBalance())
                                .amountBalance(item.getAmountBalance())
                                .build()
                );
            }
        }

        return list;
    }

    private RollListResponseDto getResponse() {

        log.info("------> Старт метода по поиску в 1с всех Рулонов под краном");

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Condition='cast(СтруктурнаяЕдиница, 'Catalog_СтруктурныеЕдиницы') eq guid'%s'')" +
                "?" +
                "$select=Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance, СуммаBalance&" +
                "$format=json", oneCProperties.getOneCGuid());

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

        log.info("------> Конец метода по поиску в 1с всех Рулонов под краном");

        return response;
    }
}


