package com.example.serviceonec.Mobile.service;

import com.example.serviceonec.Mobile.dto.input.RollMobileInputResponseDto;
import com.example.serviceonec.Mobile.dto.output.RollMobileOutputResponseDto;
import com.example.serviceonec.Mobile.dto.output.RollMobileValueOutputResponseDto;
import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.entity.*;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.StructuralUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RollMobileServiceImpl implements RollMobileService {

    private final RestClientConfig restClientConfig;
    private final OneCProperties oneCProperties;

    private final StructuralUnitRepository structuralUnitRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final BatchRepository batchRepository;
    private final CharacteristicRepository characteristicRepository;

    @Override
    public List<RollMobileInputResponseDto> getAllRolls(String userName) {

        List<RollMobileInputResponseDto> list = new ArrayList<>();

        Map<UUID, String> organizationMap = new HashMap<>();

        organizationMap.put(UUID.fromString("32c959f5-bf9a-11e5-ab21-f46d0466b92f"), "ФинКровля");
        organizationMap.put(UUID.fromString("ed447d5f-10e4-11e9-80f4-000c29aa9162"), "НК Махачкала");
        organizationMap.put(UUID.fromString("e79c6ee7-3be9-11ec-815c-000c29f4122d"), "НК Дербент");
        organizationMap.put(UUID.fromString("b125e94a-af5f-11ee-8676-a6875c54f300"), "КК Пятигорск");

        Map<UUID, String> structuralUnitMap = createStructuralUnitMap();
        Map<UUID, String> nomenclatureMap = createNomenclatureMap();
        Map<UUID, BatchEntity> batchMap = createBatchMap();
        Map<UUID, String> characteristicMap = createrCharacteristicMap();

        RollMobileOutputResponseDto response = getResponse();

        for (RollMobileValueOutputResponseDto value : response.getValue()) {
            if (value.getBatchKey().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000000")) == 0) {
                continue;
            }

            String name = nomenclatureMap.get(value.getNomenclatureKey()) +
                    " " +
                    characteristicMap.get(value.getCharacteristicKey());
            BigDecimal areaSquareMeters = value.getQuantityBalance();
            BigDecimal weightTons = BigDecimal.ZERO;
            BigDecimal pricePerTon = BigDecimal.ZERO;
            String organization = organizationMap.get(value.getOrganizationKey());

            BatchEntity batchEntity = batchMap.get(value.getBatchKey());

            if (batchEntity == null) {
                list.add(
                        RollMobileInputResponseDto.builder()
                                .name(name)
                                .manufacturer(
                                        structuralUnitMap.get(value.getStructuralUnit())
                                )
                                .organization(organization)
                                .batch(
                                        "No Name"
                                )
                                .pricePerSquareMeter(BigDecimal.valueOf(0.0))
                                .pricePerTon(pricePerTon)
                                .areaSquareMeters(areaSquareMeters)
                                .weightTons(weightTons)
                                .batchDate(LocalDate.of(2026, 12, 31))
                                .build()
                );

                continue;
            }

            BigDecimal weight = batchEntity.getWeight();
            BigDecimal pricePerSquareMeter = batchEntity.getPrice();
            BigDecimal batchQuantity= batchEntity.getQuantity();

            if (batchQuantity.compareTo(BigDecimal.ZERO) == 0) {
                log.error("Обнаружен нулевой делитель при обработке рулона: {}-{}", name, batchEntity.getDescription());

//                list.add(
//                        RollMobileInputResponseDto.builder()
//                                .name(name)
//                                .manufacturer(
//                                        structuralUnitMap.get(value.getStructuralUnit())
//                                )
//                                .organization(organization)
//                                .batch(
//                                        "No Name"
//                                )
//                                .pricePerSquareMeter(BigDecimal.valueOf(0.0))
//                                .pricePerTon(pricePerTon)
//                                .areaSquareMeters(areaSquareMeters)
//                                .weightTons(weightTons)
//                                .batchDate(LocalDate.of(2026, 12, 31))
//                                .build()
//                );
                continue;
            }

            BigDecimal coefficient = weight.divide(batchQuantity, RoundingMode.HALF_UP);

            if (weight.compareTo(BigDecimal.ZERO) > 0) {
                weightTons = areaSquareMeters.multiply(coefficient).divide(
                        BigDecimal.valueOf(1000),
                        RoundingMode.HALF_UP
                );
                pricePerTon = pricePerSquareMeter.divide(
                        coefficient,
                        RoundingMode.HALF_UP
                ).multiply(BigDecimal.valueOf(1000));
            }

            list.add(
                    RollMobileInputResponseDto.builder()
                            .name(name)
                            .manufacturer(
                                    structuralUnitMap.get(value.getStructuralUnit())
                            )
                            .organization(organization)
                            .batch(
                                    batchEntity.getDescription()
                            )
                            .pricePerSquareMeter(pricePerSquareMeter)
                            .pricePerTon(pricePerTon)
                            .areaSquareMeters(areaSquareMeters)
                            .weightTons(weightTons)
                            .batchDate(batchEntity.getBatchDate().toLocalDate())
                            .build()
            );

        }

        return list;
    }

    private RollMobileOutputResponseDto getResponse() {

        log.info("------> Старт метода по поиску в 1с всех Рулонов");

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Condition='cast(Номенклатура_Key, 'Catalog_КатегорииНоменклатуры') eq guid'%s''" +
                "?" +
                "$select=Организация_Key, СтруктурнаяЕдиница, Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance&" +
                "$format=json", oneCProperties.getOneCGuidCategory());

        RollMobileOutputResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(RollMobileOutputResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Списка рулонов"), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех Рулонов");

        return response;
    }

    private Map<UUID, String> createStructuralUnitMap() {

        List<StructuralUnitEntity> entities = structuralUnitRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (StructuralUnitEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        return dataMap;
    }

    private Map<UUID, String> createNomenclatureMap() {

        List<NomenclatureEntity> entities = nomenclatureRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (NomenclatureEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        return dataMap;

    }

    private Map<UUID, BatchEntity> createBatchMap() {

        List<BatchEntity> entities = batchRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, BatchEntity> dataMap = new HashMap<>(initialCapacity);

        for (BatchEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity);
        }

        return dataMap;
    }

    private Map<UUID, String> createrCharacteristicMap() {

        List<CharacteristicEntity> entities = characteristicRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (CharacteristicEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        return dataMap;
    }
}
