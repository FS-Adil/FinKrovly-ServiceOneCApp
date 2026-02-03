package com.example.serviceonec.service.rolllist;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.production.ProductionResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListResponseDto;
import com.example.serviceonec.model.entity.rolllist.RollListEntity;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.util.OneCGuid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Override
    public List<RollListEntity> getAllClosedRoll() {

        List<RollListEntity> list = new ArrayList<>();

        RollListResponseDto rollListResponseDto = getResponse();

        for (RollListItemResponseDto item : rollListResponseDto.getValue()) {

            if (item.getQuantityBalance().compareTo(0.0) > 0) {
                list.add(
                        RollListEntity.builder()
                                .nomenclatureName(nomenclatureRepository.findByRefKey(item.getNomenclatureKey())
                                        .getDescription())
                                .characteristicName(characteristicRepository.findByRefKey(item.getCharacteristicKey())
                                        .getDescription())
                                .batchName(UUID.randomUUID()
                                        .toString())
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
                "$format=json", OneCGuid.CLOSED_ROLL_GUID);

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

        log.info("------> Конец метода по поиску в 1с всех Закрытых рулонов");

        return response;
    }
}


