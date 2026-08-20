package com.example.serviceonec.service.costprice.structure.costcalculation;


import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.model.dto.response.MeasurementUnitItemResponseDto;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CategoriesEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendFullEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import com.example.serviceonec.model.entity.resultingincome.ResultingIncomeFullEntity;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CategoriesRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.expend.ExpendFullRepository;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import com.example.serviceonec.repository.remaining.RemainingRepository;
import com.example.serviceonec.repository.resultingincome.ResultingIncomeFullRepository;
import com.example.serviceonec.service.CategoriesService;
import com.example.serviceonec.service.MeasurementUnitService;
import com.example.serviceonec.service.invoice.InvoiceStocksService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CostCalculationFullService {

    private final ExpendFullRepository expendFullRepository;
    private final ResultingIncomeFullRepository resultingIncomeFullRepository;
    private final RemainingRepository remainingRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final CategoriesRepository categoriesRepository;

    private final InvoiceStocksService invoiceStocksService;
    private final MeasurementUnitService measurementUnitService;

    private List<ExpendFullEntity> expendFullEntityList = new ArrayList<>();

    @Getter
    private List<CostPriceControllerOutput> list = new ArrayList<>();

    private Map<UUID, Map<UUID, Map<UUID, List<ResultingIncomeFullEntity>>>> resultingIncomeFullMap = new HashMap<>();
    private Map<UUID, Map<UUID, Map<UUID, RemainingEntity>>> remainigStocksMap = new HashMap<>();

    private List<InvoiceEntity> returnInvoiceEntityList = new ArrayList<>();
    private Map<UUID, List<InvoiceStocksEntity>> dataMap = new HashMap<>();

    private Map<UUID, NomenclatureEntity> nomenclatureMap = new HashMap<>();
    private Map<UUID, String> categoriesMap = new HashMap<>();
    private Map<UUID, String> characteristicMap = new HashMap<>();
    private Map<UUID, String> batchMap = new HashMap<>();
    private Map<UUID, String> measurementUnitMap = new HashMap<>();

    public void findAllExpendFullEntity() {
        log.debug("🔍 Поиск всех расходных накладных в БД");
        this.expendFullEntityList = expendFullRepository.findAll();
    }

    public void findCostPrice() {
        log.info("🚀 ===== НАЧАЛО РАСЧЕТА СЕБЕСТОИМОСТИ =====");

        long methodStartTime = System.currentTimeMillis();

        log.info("  - Загрузка номенклатуры...");
        this.nomenclatureMap = createMapForNomenclature();

        log.info(" -  Загрузка категории");
        this.categoriesMap = createMapForCategories();

        log.info("  - Загрузка характеристик...");
        this.characteristicMap = createMapForCharacteristic();

        log.info("  - Загрузка партий...");
        this.batchMap = createMapForBatch();

        log.info("  - Загрузка Единиц Измерения...");
        this.measurementUnitMap = createMapForMeasurementUnit();

        this.list.clear();
        log.info("Список с расчетом себестоимости очищен");

        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);
        AtomicInteger foundWithCost = new AtomicInteger(0);
        AtomicInteger notFoundNomenclature = new AtomicInteger(0);
        AtomicInteger notFoundCharacteristic = new AtomicInteger(0);
        AtomicInteger notFoundBatch = new AtomicInteger(0);
        AtomicInteger zeroCost = new AtomicInteger(0);
        AtomicLong totalQuantity = new AtomicLong(0);

        for (ExpendFullEntity expend : this.expendFullEntityList) {
            int expendNum = processedExpend.incrementAndGet();
            UUID expendRefKey = expend.getRefKey();

            if (expendNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} расходников", expendNum, this.expendFullEntityList.size());
            }

            UUID nomenclatureKey = expend.getNomenclatureKey();
            UUID characteristicKey = expend.getCharacteristicKey();
            UUID batchKey = expend.getBatchKey();
            UUID measurementUnitKey = expend.getMeasurementUnitKey();

            String number = expend.getNumber();
            String refKey = expendRefKey.toString();

            NomenclatureEntity newNomenclatureEntity = new NomenclatureEntity();
            newNomenclatureEntity.setCategoriesKey(UUID.randomUUID());
            newNomenclatureEntity.setDescription("Не найдено");

            NomenclatureEntity nomenclatureEntity = nomenclatureMap.getOrDefault(nomenclatureKey,
                            newNomenclatureEntity);

            String name = nomenclatureEntity.getDescription();
            UUID categoriesKey = nomenclatureEntity.getCategoriesKey();
            String categories = categoriesMap.getOrDefault(categoriesKey, "Не найдено");// необходимо определить категорию, через номенклатуру
            String characteristic = characteristicMap.getOrDefault(characteristicKey, "Не найдено");
            String batch = batchMap.getOrDefault(batchKey, "Не найдено");
            String measurementUnit = measurementUnitMap.getOrDefault(measurementUnitKey, "Не найдено");
            BigDecimal price = expend.getPrice();
            BigDecimal quantity = expend.getQuantity().setScale(3, RoundingMode.HALF_UP);

            totalQuantity.addAndGet(quantity.longValue());

            // Проверка наличия в приходниках
            if (!resultingIncomeFullMap.containsKey(nomenclatureKey)) {
                notFoundNomenclature.incrementAndGet();
                addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity, price, BigDecimal.ZERO);
                continue;
            }
            if (!resultingIncomeFullMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                notFoundCharacteristic.incrementAndGet();
                addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity, price, BigDecimal.ZERO);
                continue;
            }
            if (!resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                notFoundBatch.incrementAndGet();
                addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity, price, BigDecimal.ZERO);
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
                    addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit,
                            quantity,
                            price,
                            BigDecimal.ZERO
                    );
                } else {
                    foundWithCost.incrementAndGet();
                    addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit,
                            quantity,
                            price,
                            cost
                    );
                }
            }
        }

        long totalTime = System.currentTimeMillis() - methodStartTime;

        log.info("📊 ===== СТАТИСТИКА РАСЧЕТА =====");
        log.info("📦 Всего расходников: {}", expendFullEntityList.size());
        log.info("📦 Всего позиций запасов: {}", processedStocks.get());
        log.info("📦 Общее количество товара: {}", totalQuantity.get());
        log.info("");
        log.info("✅ Найдено с себестоимостью: {}", foundWithCost.get());
        log.info("❌ Не найдено номенклатуры: {}", notFoundNomenclature.get());
        log.info("❌ Не найдено характеристик: {}", notFoundCharacteristic.get());
        log.info("❌ Не найдено партий: {}", notFoundBatch.get());
        log.info("❌ Нулевая себестоимость: {}", zeroCost.get());
        log.info("");
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);

    }

    public void returnExpendStocks(LocalDateTime startDate, LocalDateTime endDate) {
        // получаем все приходники с возвратом покупателя за отчетный период
        // формируем мапу исходя из списка выше и запасов приходных накладных
        // добавляем в общий список по продажам со знаком минус
        log.info("🚀 ===== Добавляем, в итоговый список продаж, ВОЗВРАТЫ от покупателей =====");

        long methodStartTime = System.currentTimeMillis();

        getAllReturnInvoiceStocks(startDate, endDate);

        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);

        AtomicInteger notFoundNomenclature = new AtomicInteger(0);
        AtomicInteger notFoundCharacteristic = new AtomicInteger(0);
        AtomicInteger notFoundBatch = new AtomicInteger(0);



        if (this.returnInvoiceEntityList.isEmpty()) {
            long totalTime = System.currentTimeMillis() - methodStartTime;

            log.info("📊 ===== СТАТИСТИКА ВОЗВРАТЫ от покупателей =====");
            log.info("📦 Всего возвратов: {}", processedExpend.get());
            log.info("📦 Всего Запасов возвратов: {}", processedStocks.get());
            log.info("❌ Не найдено номенклатуры: {}", notFoundNomenclature.get());
            log.info("❌ Не найдено характеристик: {}", notFoundCharacteristic.get());
            log.info("❌ Не найдено партий: {}", notFoundBatch.get());
            log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
            return;
        }

        for (InvoiceEntity entity : this.returnInvoiceEntityList) {
            processedExpend.incrementAndGet();

            UUID uuid = entity.getRefKey();
            List<InvoiceStocksEntity> stocks = this.dataMap.get(uuid);
            if (stocks == null) {
                continue;
            }
            for (InvoiceStocksEntity invoiceStocksEntity : stocks) {
                processedStocks.incrementAndGet();

                String refKey = uuid.toString();
                String number = entity.getNumber();
                UUID nomenclatureKey = invoiceStocksEntity.getNomenclatureKey();

                NomenclatureEntity newNomenclatureEntity = new NomenclatureEntity();
                newNomenclatureEntity.setCategoriesKey(UUID.randomUUID());
                newNomenclatureEntity.setDescription("Не найдено");

                NomenclatureEntity nomenclatureEntity = nomenclatureMap.getOrDefault(nomenclatureKey,
                        newNomenclatureEntity);
                String name = nomenclatureEntity.getDescription();
                UUID categoriesKey = nomenclatureEntity.getCategoriesKey();
                String categories = categoriesMap.getOrDefault(categoriesKey, "Не найдено");
                UUID characteristicKey = invoiceStocksEntity.getCharacteristicKey();
                String characteristic = this.characteristicMap.get(characteristicKey);
                UUID batchKey = invoiceStocksEntity.getBatchKey();
                String batch = this.batchMap.get(batchKey);
                String measurementUnit = this.measurementUnitMap.get(UUID.randomUUID());
                BigDecimal quantity = invoiceStocksEntity.getQuantity();
                BigDecimal price = invoiceStocksEntity.getPrice();

                // Проверка наличия в приходниках
                if (!resultingIncomeFullMap.containsKey(nomenclatureKey)) {
                    notFoundNomenclature.incrementAndGet();
                    addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity.negate(), price, BigDecimal.ZERO);
                    continue;
                }
                if (!resultingIncomeFullMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    notFoundCharacteristic.incrementAndGet();
                    addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity.negate(), price, BigDecimal.ZERO);
                    continue;
                }
                if (!resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                    notFoundBatch.incrementAndGet();
                    addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit, quantity.negate(), price, BigDecimal.ZERO);
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
                        addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit,
                                quantity.negate(),
                                price,
                                BigDecimal.ZERO
                        );
                    } else {
                        addToResult(this.list, refKey, number, name, characteristic, batch, categories, measurementUnit,
                                quantity.negate(),
                                price,
                                cost
                        );
                    }
                }
            }
        }
        long totalTime = System.currentTimeMillis() - methodStartTime;

        log.info("📊 ===== СТАТИСТИКА ВОЗВРАТЫ от покупателей =====");
        log.info("📦 Всего возвратов: {}", processedExpend.get());
        log.info("📦 Всего Запасов возвратов: {}", processedStocks.get());
        log.info("❌ Не найдено номенклатуры: {}", notFoundNomenclature.get());
        log.info("❌ Не найдено характеристик: {}", notFoundCharacteristic.get());
        log.info("❌ Не найдено партий: {}", notFoundBatch.get());
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
    }

    private void getAllReturnInvoiceStocks (LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("🔍 Поиск всех приходных накладных в БД отсортированных по дате и по типу");

        if(!this.returnInvoiceEntityList.isEmpty()) {
            this.returnInvoiceEntityList.clear();
        }

        String operationType = "ВозвратОтПокупателя";
        this.returnInvoiceEntityList = invoiceRepository.findAllByOperationTypeAndDateRange(
                operationType,
                startDate,
                endDate
        );

        if (this.returnInvoiceEntityList.isEmpty()) {
            return;
        }

        log.debug("🔍 Загрузка запасов приходных накладных");
        List<UUID> refKeys = this.returnInvoiceEntityList.stream()
                .map(InvoiceEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (refKeys.isEmpty()) {
            log.warn("⚠️ Не найдено приходных накладных с типом операции: {}", operationType);
        }

        log.debug("Найдено {} приходных накладных с типом операции '{}'", refKeys.size(), operationType);


        List<InvoiceStocksEntity> allStocks = invoiceStocksRepository.findAllByRefKeyIn(refKeys);

        log.debug("Загружено {} записей запасов из БД", allStocks.size());

        Set<UUID> foundRefKeys = allStocks.stream()
                .map(InvoiceStocksEntity::getRefKey)
                .collect(Collectors.toSet());

        Set<UUID> missingRefKeys = new HashSet<>(refKeys);
        missingRefKeys.removeAll(foundRefKeys);

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} приходников без запасов, загружаем из 1С...", missingRefKeys.size());
            try {
                List<InvoiceStocksEntity> foundMissing = invoiceStocksService
                        .findInvoiceStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    allStocks.addAll(foundMissing);
                    log.info("✅ Загружено {} записей из 1С", foundMissing.size());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < refKeys.size(); i++) {
            orderMap.put(refKeys.get(i), i);
        }

        allStocks.sort(Comparator.comparing(s -> orderMap.get(s.getRefKey())));

        this.dataMap = allStocks.stream()
                .collect(Collectors.groupingBy(InvoiceStocksEntity::getRefKey));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.dataMap.size());
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
        log.debug("🔍 Расчет себестоимости: номенклатура={}<->{}, характеристика={}<->{}, партия={}<->{}, количество={}",
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

    private void addToResult(List<CostPriceControllerOutput> list, String refKey, String number,
                             String name, String characteristic, String batch, String categories, String measurementUnit,
                             BigDecimal quantity, BigDecimal price, BigDecimal cost) {
        list.add(CostPriceControllerOutput.builder()
                .refKey(refKey)
                .number(number)
                .name(name)
                .characteristic(characteristic)
                .batch(batch)
                        .categories(categories)
                .measurementUnit(measurementUnit)
                .quantity(quantity)
                .price(price)
                .cost(cost)
                .build());
    }

    public void findAllResultingIncomeFullEntity() {
        log.debug("🔍 Загрузка всех приходных накладных и производств");

        this.resultingIncomeFullMap.clear();

        List<ResultingIncomeFullEntity> resultingIncomeFullEntityList = resultingIncomeFullRepository.findAllByOrderByDateDesc();

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

    public void findAllRemainingStocks() {
        this.remainigStocksMap.clear();
        List<RemainingEntity> remainingEntityList = remainingRepository.findAll();

        for (RemainingEntity entity : remainingEntityList) {
            // Создаем КОПИЮ!
            RemainingEntity copy = new RemainingEntity();
            BeanUtils.copyProperties(entity, copy);

            this.remainigStocksMap
                    .computeIfAbsent(copy.getNomenclatureKey(), k -> new HashMap<>())
                    .computeIfAbsent(copy.getCharacteristicKey(), k -> new HashMap<>())
                    .put(copy.getBatchKey(), copy);
        }
    }

    public void findAllRemainingStocksOld() {
        log.debug("🔍 Загрузка остатков из БД");

        this.remainigStocksMap.clear();

        List<RemainingEntity> remainingEntityList = remainingRepository.findAll();


        for (RemainingEntity entity : remainingEntityList) {
            Map<UUID, Map<UUID, RemainingEntity>> mapLevel2 = new HashMap<>();
            Map<UUID, RemainingEntity> mapLevel1 = new HashMap<>();

            UUID id_nom = entity.getNomenclatureKey();
            UUID id_char = entity.getCharacteristicKey();
            UUID id_b = entity.getBatchKey();

            mapLevel1.put(id_b, entity);
            mapLevel2.put(id_char, mapLevel1);
            this.remainigStocksMap.put(id_nom, mapLevel2);
        }

        log.debug("✅ Остатки загружены, уникальных номенклатур: {}", this.remainigStocksMap.size());
    }

    public void updateResultingIncome() {
        log.debug("🔄 Корректировка ПРИХОДОВ с учетом остатков");

        AtomicInteger processedNom = new AtomicInteger(0);
        AtomicInteger processedChar = new AtomicInteger(0);
        AtomicInteger processedBatch = new AtomicInteger(0);
        AtomicInteger adjustedRecords = new AtomicInteger(0);

        List<UUID> keysListNom = new ArrayList<>(this.resultingIncomeFullMap.keySet());

        for (UUID nomenclatureKey : keysListNom) {
            processedNom.incrementAndGet();

            if (!this.remainigStocksMap.containsKey(nomenclatureKey)) {
                log.debug("  - Номенклатура {}: нет в остатках", nomenclatureKey);
                continue;
            }

            List<UUID> keysListChar = new ArrayList<>(this.resultingIncomeFullMap.get(nomenclatureKey).keySet());

            for (UUID characteristicKey : keysListChar) {
                processedChar.incrementAndGet();

                if (!this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    log.debug("  - Характеристика {}: нет в остатках", characteristicKey);
                    continue;
                }

                List<UUID> keysListB = new ArrayList<>(this.resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).keySet());

                for (UUID batchKey : keysListB) {
                    processedBatch.incrementAndGet();

                    if (!this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                        log.debug("  - Партия {}: нет в остатках", batchKey);
                        continue;
                    }

                    List<ResultingIncomeFullEntity> invoiceStocksList = this.resultingIncomeFullMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    RemainingEntity remainingItem = this.remainigStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    BigDecimal remainingStockQuantity = remainingItem.getQuantityBalance().setScale(3, RoundingMode.HALF_UP);
                    BigDecimal originalRemaining = remainingStockQuantity;

                    for (int i = 0; i < invoiceStocksList.size(); i++) {
                        ResultingIncomeFullEntity entity = invoiceStocksList.get(i);
                        BigDecimal invoiceQuantity = entity.getQuantity();

                        if (invoiceQuantity.compareTo(remainingStockQuantity) <= 0) {
                            // Полностью списываем приходник
                            entity.setQuantity(BigDecimal.ZERO);
                            remainingStockQuantity = remainingStockQuantity.subtract(invoiceQuantity);
                            adjustedRecords.incrementAndGet();

                            invoiceStocksList.set(i, entity);

                            log.debug("{}   - Полное списание: приходник {} ({}), остаток после: {}",
                                    nomenclatureKey, i, invoiceQuantity, remainingStockQuantity);
                        } else {
                            // Частичное списание
                            entity.setQuantity(invoiceQuantity.subtract(remainingStockQuantity));
                            remainingStockQuantity = BigDecimal.valueOf(0.0);
                            adjustedRecords.incrementAndGet();

                            log.debug("{}   - Частичное списание: приходник {} ({} -> {}), остаток обнулен",
                                    nomenclatureKey, i, invoiceQuantity, entity.getQuantity());

                            invoiceStocksList.set(i, entity);

                            //Обновляем приходники
                            this.resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
                            log.debug("  - Партия {}: скорректировано, приходник изменен с {} на {}",
                                    batchKey, invoiceQuantity, entity.getQuantity());
                            break;
                        }
                    }

                    // Обновляем остаток
                    remainingItem.setQuantityBalance(remainingStockQuantity);
                    this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItem);

                    log.debug("  - Партия {}: скорректировано, остаток изменен с {} на {}",
                            batchKey, originalRemaining, remainingStockQuantity);
                }
            }
        }
    }

    private Map<UUID, NomenclatureEntity> createMapForNomenclature() {
        log.debug("🔍 Создание справочника номенклатуры");
        List<NomenclatureEntity> entities = nomenclatureRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, NomenclatureEntity> dataMap = new HashMap<>(initialCapacity);

        for (NomenclatureEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity);
        }

        log.debug("✅ Справочник номенклатуры создан, записей: {}", dataMap.size());
        return dataMap;
    }

    private Map<UUID, String> createMapForCategories() {
        log.debug("🔍 Создание справочника категории номенклатуры");
        List<CategoriesEntity> entities = categoriesRepository.findAll();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (CategoriesEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.debug("✅ Справочник категории номенклатуры создан, записей: {}", dataMap.size());
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

    private Map<UUID, String> createMapForMeasurementUnit() {
        log.debug("🔍 Создание справочника Ед");
        List<MeasurementUnitItemResponseDto> entities = measurementUnitService.getAllMeasurementUnit();

        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (MeasurementUnitItemResponseDto entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.debug("✅ Справочник Ед. создан, записей: {}", dataMap.size());
        return dataMap;
    }

}

