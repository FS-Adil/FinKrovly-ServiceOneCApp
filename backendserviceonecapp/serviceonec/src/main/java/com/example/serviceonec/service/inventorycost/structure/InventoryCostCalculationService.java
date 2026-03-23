package com.example.serviceonec.service.inventorycost.structure;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.controller.inventorycost.output.InventoryCostControllerOutput;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendFullEntity;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import com.example.serviceonec.model.entity.resultingincome.ResultingIncomeFullEntity;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.remaining.RemainingRepository;
import com.example.serviceonec.repository.resultingincome.ResultingIncomeFullRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryCostCalculationService {

    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;
    private final ResultingIncomeFullRepository resultingIncomeFullRepository;
    private final RemainingRepository remainingRepository;

    @Getter
    private List<InventoryCostControllerOutput> list = new ArrayList<>();

    private Map<UUID, Map<UUID, Map<UUID, List<ResultingIncomeFullEntity>>>> resultingIncomeFullMap = new HashMap<>();
    private List<RemainingEntity> remainingEntityList = new ArrayList<>();

    public void findAllRemainingStocks() {
        log.debug("🔍 Поиск всех остатков в БД");
        this.remainingEntityList = remainingRepository.findAll();
    }

    public void findInventoryCostPrice(UUID organizationId) {
        log.info("  - Загрузка номенклатуры...");
        Map<UUID, String> nomenclatureMap = createMapForNomenclature();

        log.info("  - Загрузка характеристик...");
        Map<UUID, String> characteristicMap = createMapForCharacteristic();

        log.info("  - Загрузка партий...");
        Map<UUID, String> batchMap = createMapForBatch();

        this.list.clear();
        log.info("Список с расчетом себестоимости очищен");

        AtomicInteger processedRemaining = new AtomicInteger(0);
        AtomicInteger foundWithCost = new AtomicInteger(0);
        AtomicInteger notFoundNomenclature = new AtomicInteger(0);
        AtomicInteger notFoundCharacteristic = new AtomicInteger(0);
        AtomicInteger notFoundBatch = new AtomicInteger(0);
        AtomicInteger zeroCost = new AtomicInteger(0);

        for (RemainingEntity entity : this.remainingEntityList) {

            if (entity.getOrganizationKey().compareTo(organizationId) != 0) {
                continue;
            }

            if (entity.getQuantityBalance().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            int expendNum = processedRemaining.incrementAndGet();

            if (expendNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} остатков", expendNum, this.remainingEntityList.size());
            }

            UUID nomenclatureKey = entity.getNomenclatureKey();
            UUID characteristicKey = entity.getCharacteristicKey();
            UUID batchKey = entity.getBatchKey();

            String orgId = organizationId.toString();

            String name = nomenclatureMap.getOrDefault(nomenclatureKey, "Не найдено");
            String characteristic = characteristicMap.getOrDefault(characteristicKey, "Не найдено");
            String batch = batchMap.getOrDefault(batchKey, "Не найдено");
            BigDecimal quantity = entity.getQuantityBalance().setScale(3, RoundingMode.HALF_UP);


            // Проверка наличия в приходниках
            if (!resultingIncomeFullMap.containsKey(nomenclatureKey)) {
                notFoundNomenclature.incrementAndGet();
                addToResult(this.list, orgId, name, characteristic, batch, quantity, BigDecimal.ZERO);
                continue;
            }
            if (!resultingIncomeFullMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                notFoundCharacteristic.incrementAndGet();
                addToResult(this.list, orgId, name, characteristic, batch, quantity, BigDecimal.ZERO);
                continue;
            }
            if (!resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                notFoundBatch.incrementAndGet();
                addToResult(this.list, orgId, name, characteristic, batch, quantity, BigDecimal.ZERO);
                continue;
            }

            List<Map<String, BigDecimal>> listCost = getMapCostForNomenclature(
                    nomenclatureKey,
                    name,
                    characteristicKey,
                    characteristic,
                    batchKey,
                    batch,
                    quantity
            );

            for (Map<String, BigDecimal> map : listCost) {
                BigDecimal cost = map.get("cost");
                quantity = map.get("quantity");
                if (cost.compareTo(BigDecimal.ZERO) == 0) {
                    zeroCost.incrementAndGet();
                    addToResult(this.list, orgId, name, characteristic, batch,
                            quantity,
                            BigDecimal.ZERO
                    );
                } else {
                    foundWithCost.incrementAndGet();
                    addToResult(this.list, orgId, name, characteristic, batch,
                            quantity,
                            cost
                    );
                }
            }
        }



        log.info("📊 ===== СТАТИСТИКА РАСЧЕТА =====");
        log.info("📦 Всего Остатков: {}", this.remainingEntityList.size());

        log.info("");
        log.info("✅ Найдено с себестоимостью: {}", foundWithCost.get());
        log.info("❌ Не найдено номенклатуры: {}", notFoundNomenclature.get());
        log.info("❌ Не найдено характеристик: {}", notFoundCharacteristic.get());
        log.info("❌ Не найдено партий: {}", notFoundBatch.get());
        log.info("❌ Нулевая себестоимость: {}", zeroCost.get());
    }

    private List<Map<String, BigDecimal>> getMapCostForNomenclature(
            UUID nomenclatureKey,
            String name,
            UUID characteristicKey,
            String characteristic,
            UUID batchKey,
            String batch,
            BigDecimal quantity
    ) {
        log.debug("🔍 Расчет себестоимости остатков: номенклатура={}<->{}, характеристика={}<->{}, партия={}<->{}, количество={}",
                nomenclatureKey,
                name,
                characteristicKey,
                characteristic,
                batchKey,
                batch,
                quantity);

        List<ResultingIncomeFullEntity> invoiceStocksList = this.resultingIncomeFullMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        List<Map<String, BigDecimal>> list = new ArrayList<>();

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            HashMap<String, BigDecimal> map = new HashMap<>();

            ResultingIncomeFullEntity entity = invoiceStocksList.get(i);

            BigDecimal invoiceQuantity = entity.getQuantity().setScale(3, RoundingMode.HALF_UP);

            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("❌ Количество не найдена цена={}", quantity);
                break;
            }
            if (invoiceQuantity.compareTo(quantity) >= 0) {
                BigDecimal newQuantity = invoiceQuantity.subtract(quantity).setScale(3, RoundingMode.HALF_UP);

                entity.setQuantity(newQuantity);

                invoiceStocksList.set(i, entity);
                this.resultingIncomeFullMap
                        .get(nomenclatureKey)
                        .get(characteristicKey)
                        .replace(batchKey, invoiceStocksList);

                log.debug("✅ Найдена себестоимость: цена={}, остаток после списания={}",
                        entity.getPrice(), newQuantity);
                map.put("quantity", quantity);
                map.put("cost", entity.getPrice());
                list.add(map);
                return list;
            } else if (invoiceQuantity.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("⚠️ Недостаточно количества в приходнике: требуется {}, доступно {}",
                        quantity, invoiceQuantity);
            } else {
                log.debug("⚠️ Недостаточно количества в приходнике: требуется {}, доступно {}",
                        quantity, invoiceQuantity);

                quantity = quantity.subtract(invoiceQuantity);

                map.put("quantity", invoiceQuantity);
                map.put("cost", entity.getPrice());
                list.add(map);
                log.debug("✅ Найдена себестоимость: цена={}, количество={}",
                        entity.getPrice(), invoiceQuantity);

                entity.setQuantity(BigDecimal.ZERO);

                invoiceStocksList.set(i, entity);
                this.resultingIncomeFullMap
                        .get(nomenclatureKey)
                        .get(characteristicKey)
                        .replace(batchKey, invoiceStocksList);
            }
        }

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            HashMap<String, BigDecimal> map = new HashMap<>();

            map.put("quantity", quantity);
            map.put("cost", BigDecimal.ZERO);
            list.add(map);
            log.debug("❌ Себестоимость не найдена");
        }
        return list;
    }

    private void addToResult(List<InventoryCostControllerOutput> list, String refKey,
                             String name, String characteristic, String batch,
                             BigDecimal quantity, BigDecimal cost) {
        list.add(InventoryCostControllerOutput.builder()
                .refKey(refKey)
                .name(name)
                .characteristic(characteristic)
                .batch(batch)
                .cost(cost)
                .quantity(quantity)
                .build()
        );
    }

    public void findAllResultingIncomeFullEntity() {
        log.debug("🔍 Загрузка всех приходных накладных и производств");

        this.resultingIncomeFullMap.clear();

        List<ResultingIncomeFullEntity> resultingIncomeFullEntityList = resultingIncomeFullRepository.findAll();

        if (resultingIncomeFullEntityList.isEmpty()) {
            log.warn("⚠️ Не найдено данных в таблице resulting_income_full");
            return;
        }

        log.debug("Загружено {} записей запасов из БД", resultingIncomeFullEntityList.size());

        this.resultingIncomeFullMap = resultingIncomeFullEntityList.stream()
                .collect(Collectors.groupingBy(
                        ResultingIncomeFullEntity::getNomenclatureKey,
                        Collectors.groupingBy(
                                ResultingIncomeFullEntity::getCharacteristicKey,
                                Collectors.groupingBy(
                                        ResultingIncomeFullEntity::getBatchKey,
                                        Collectors.toList()
                                )
                        )
                ));

        log.debug("✅ Мапа ПРИХОДОВ создана: {} номенклатур", this.resultingIncomeFullMap.size());
    }

    private Map<UUID, String> createMapForNomenclature() {
        log.debug("🔍 Создание справочника номенклатуры");
        List<NomenclatureEntity> entities = nomenclatureRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (NomenclatureEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.debug("✅ Справочник номенклатуры создан, записей: {}", dataMap.size());
        return dataMap;
    }

    private Map<UUID, String> createMapForCharacteristic() {
        log.debug("🔍 Создание справочника характеристик");
        List<CharacteristicEntity> entities = characteristicRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (CharacteristicEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.debug("✅ Справочник характеристик создан, записей: {}", dataMap.size());
        return dataMap;
    }

    private Map<UUID, String> createMapForBatch() {
        log.debug("🔍 Создание справочника партий");
        List<BatchEntity> entities = batchRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (BatchEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.debug("✅ Справочник партий создан, записей: {}", dataMap.size());
        return dataMap;
    }
}
