package com.example.serviceonec.service.costprice;

import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.model.dto.response.costprice.RemainingItemStockResponseDto;
import com.example.serviceonec.model.dto.response.costprice.RemainingStockResponseDto;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import com.example.serviceonec.service.expend.ExpendStocksService;
import com.example.serviceonec.service.invoice.InvoiceStocksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostPriceServiceImpl implements CostPriceService {

    private Map<UUID, Map<UUID, Map<UUID, List<InvoiceStocksEntity>>>> invoiceStocksMap = new HashMap<>();
    private final Map<UUID, Map<UUID, Map<UUID, RemainingItemStockResponseDto>>> remainigStocksMap = new HashMap<>();

    private final ExpendRepository expendRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;

    private final OneCProperties oneCProperties;
    private final RestClientConfig restClientConfig;

    private final ExpendStocksService expendStocksService;
    private final InvoiceStocksService invoiceStocksService;

    @Override
    public List<CostPriceControllerOutput> getAllCostPrice(
            UUID organizationId,
            LocalDateTime endDate
    ) {
        long methodStartTime = System.currentTimeMillis();
        log.info("🚀 ===== НАЧАЛО РАСЧЕТА СЕБЕСТОИМОСТИ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Дата окончания периода: {}", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<CostPriceControllerOutput> list = new ArrayList<>();

        // Шаг 1: Загрузка расходников
        log.info("📥 Шаг 1/7: Загрузка расходных накладных из БД...");
        long stepStart = System.currentTimeMillis();
        List<ExpendEntity> expendList = findAllExpend();
        log.info("✅ Загружено {} расходных накладных за {} мс", expendList.size(), System.currentTimeMillis() - stepStart);

        // Шаг 2: Загрузка запасов расходников
        log.info("📥 Шаг 2/7: Загрузка запасов расходных накладных...");
        stepStart = System.currentTimeMillis();
        Map<UUID, List<ExpendStocksEntity>> expendStocksMap = createMapForExpendStocks(expendList);
        int totalExpendStocks = expendStocksMap.values().stream().mapToInt(List::size).sum();
        log.info("✅ Загружено {} записей запасов расходников для {} документов за {} мс",
                totalExpendStocks, expendStocksMap.size(), System.currentTimeMillis() - stepStart);

        // Шаг 3: Загрузка запасов приходников
        log.info("📥 Шаг 3/7: Загрузка запасов приходных накладных...");
        stepStart = System.currentTimeMillis();
        createMapForInvoiceStocks();
        int totalInvoiceStocks = invoiceStocksMap.values().stream()
                .flatMap(m1 -> m1.values().stream())
                .mapToInt(m2 -> m2.values().stream().mapToInt(List::size).sum())
                .sum();
        log.info("✅ Загружено {} записей запасов приходников за {} мс", totalInvoiceStocks, System.currentTimeMillis() - stepStart);

        // Шаг 4: Загрузка остатков из 1С
        log.info("📥 Шаг 4/7: Загрузка остатков товаров из 1С...");
        stepStart = System.currentTimeMillis();
        createMapForRemainigStocks(organizationId, endDate);
        int totalRemaining = remainigStocksMap.values().stream()
                .flatMap(m1 -> m1.values().stream())
                .mapToInt(m2 -> m2.size())
                .sum();
        log.info("✅ Загружено {} записей остатков за {} мс", totalRemaining, System.currentTimeMillis() - stepStart);

        // Шаг 5: Обновление приходников с учетом остатков
        log.info("🔄 Шаг 5/7: Корректировка приходников с учетом остатков на складе...");
        stepStart = System.currentTimeMillis();
        updateMapForInvoiceStocks();
        log.info("✅ Корректировка завершена за {} мс", System.currentTimeMillis() - stepStart);

        // Шаг 6: Загрузка справочников
        log.info("📚 Шаг 6/7: Загрузка справочных данных...");
        stepStart = System.currentTimeMillis();

        log.info("  - Загрузка номенклатуры...");
        Map<UUID, String> nomenclatureMap = createMapForNomenclature();

        log.info("  - Загрузка характеристик...");
        Map<UUID, String> characteristicMap = createMapForCharacteristic();

        log.info("  - Загрузка партий...");
        Map<UUID, String> batchMap = createMapForBatch();

        log.info("✅ Справочники загружены за {} мс", System.currentTimeMillis() - stepStart);

        // Шаг 7: Расчет себестоимости
        log.info("🧮 Шаг 7/7: Расчет себестоимости...");
        stepStart = System.currentTimeMillis();

        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);
        AtomicInteger foundWithCost = new AtomicInteger(0);
        AtomicInteger notFoundNomenclature = new AtomicInteger(0);
        AtomicInteger notFoundCharacteristic = new AtomicInteger(0);
        AtomicInteger notFoundBatch = new AtomicInteger(0);
        AtomicInteger zeroCost = new AtomicInteger(0);
        AtomicLong totalQuantity = new AtomicLong(0);

        for (ExpendEntity expend : expendList) {
            int expendNum = processedExpend.incrementAndGet();
            UUID expendRefKey = expend.getRefKey();

            if (expendNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} расходников", expendNum, expendList.size());
            }

            if (!expendStocksMap.containsKey(expendRefKey)) {
                log.debug("⚠️ Расходник {}: запасы не найдены", expendRefKey);
                continue;
            }

            for (ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)) {
                processedStocks.incrementAndGet();

                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                UUID characteristicKey = expendStocksEntity.getCharacteristicKey();
                UUID batchKey = expendStocksEntity.getBatchKey();

                String number = expend.getNumber();
                String refKey = expendRefKey.toString();
                String name = nomenclatureMap.getOrDefault(nomenclatureKey, "Не найдено");
                String characteristic = characteristicMap.getOrDefault(characteristicKey, "Не найдено");
                String batch = batchMap.getOrDefault(batchKey, "Не найдено");
                BigDecimal price = expendStocksEntity.getPrice();
                BigDecimal quantity = expendStocksEntity.getQuantity().setScale(3, RoundingMode.HALF_UP);

                totalQuantity.addAndGet(quantity.longValue());

                // Проверка наличия в приходниках
                if (!invoiceStocksMap.containsKey(nomenclatureKey)) {
                    notFoundNomenclature.incrementAndGet();
                    addToResult(list, refKey, number, name, characteristic, batch, quantity, price, BigDecimal.ZERO);
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    notFoundCharacteristic.incrementAndGet();
                    addToResult(list, refKey, number, name, characteristic, batch, quantity, price, BigDecimal.ZERO);
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                    notFoundBatch.incrementAndGet();
                    addToResult(list, refKey, number, name, characteristic, batch, quantity, price, BigDecimal.ZERO);
                    continue;
                }

//                BigDecimal cost = getCostForNomenclature(
//                        nomenclatureKey,
//                        name,
//                        characteristicKey,
//                        characteristic,
//                        batchKey,
//                        batch,
//                        quantity
//                );

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
                        addToResult(list, refKey, number, name, characteristic, batch,
                                quantity,
                                price,
                                BigDecimal.ZERO
                        );
                    } else {
                        foundWithCost.incrementAndGet();
                        addToResult(list, refKey, number, name, characteristic, batch,
                                quantity,
                                price,
                                cost
                        );
                    }
                }
            }
        }

        long calculationTime = System.currentTimeMillis() - stepStart;
        long totalTime = System.currentTimeMillis() - methodStartTime;

        log.info("📊 ===== СТАТИСТИКА РАСЧЕТА =====");
        log.info("📦 Всего расходников: {}", expendList.size());
        log.info("📦 Всего позиций запасов: {}", processedStocks.get());
        log.info("📦 Общее количество товара: {}", totalQuantity.get());
        log.info("");
        log.info("✅ Найдено с себестоимостью: {}", foundWithCost.get());
        log.info("❌ Не найдено номенклатуры: {}", notFoundNomenclature.get());
        log.info("❌ Не найдено характеристик: {}", notFoundCharacteristic.get());
        log.info("❌ Не найдено партий: {}", notFoundBatch.get());
        log.info("❌ Нулевая себестоимость: {}", zeroCost.get());
        log.info("");
        log.info("⏱️ Время расчета: {} мс ({} сек)", calculationTime, calculationTime / 1000);
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("📈 Скорость обработки: {} позиций/сек",
                processedStocks.get() / (calculationTime / 1000 > 0 ? calculationTime / 1000 : 1));

        // Агрегация результатов
        log.info("🔄 Агрегация результатов...");
        stepStart = System.currentTimeMillis();
        List<CostPriceControllerOutput> aggregated = aggregateOnlyFast(list);
        log.info("✅ Агрегация завершена за {} мс, получено {} уникальных записей",
                System.currentTimeMillis() - stepStart, aggregated.size());

        log.info("🏁 ===== ЗАВЕРШЕНИЕ РАСЧЕТА СЕБЕСТОИМОСТИ =====");
        return aggregated;
    }

    private void addToResult(List<CostPriceControllerOutput> list, String refKey, String number,
                             String name, String characteristic, String batch,
                             BigDecimal quantity, BigDecimal price, BigDecimal cost) {
        list.add(CostPriceControllerOutput.builder()
                .refKey(refKey)
                .number(number)
                .name(name)
                .characteristic(characteristic)
                .batch(batch)
                .quantity(quantity)
                .price(price)
                .cost(cost)
                .build());
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

        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        List<Map<String, BigDecimal>> list = new ArrayList<>();

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            HashMap<String, BigDecimal> map = new HashMap<>();

            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            BigDecimal invoiceQuantity = entity.getQuantity().setScale(3, RoundingMode.HALF_UP);

            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("❌ Количество не найдена цена={}", quantity);
                break;
            }
            if (invoiceQuantity.compareTo(quantity) >= 0) {
                BigDecimal newQuantity = invoiceQuantity.subtract(quantity).setScale(3, RoundingMode.HALF_UP);

                entity.setQuantity(newQuantity);

                invoiceStocksList.set(i, entity);
                this.invoiceStocksMap
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
                this.invoiceStocksMap
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

    private BigDecimal getCostForNomenclature(
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

        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            BigDecimal invoiceQuantity = entity.getQuantity().setScale(3, RoundingMode.HALF_UP);

            if (invoiceQuantity.compareTo(quantity) >= 0) {
                BigDecimal newQuantity = invoiceQuantity.subtract(quantity).setScale(3, RoundingMode.HALF_UP);
                entity.setQuantity(newQuantity);
                invoiceStocksList.set(i, entity);
                this.invoiceStocksMap
                        .get(nomenclatureKey)
                        .get(characteristicKey)
                        .replace(batchKey, invoiceStocksList);

                log.debug("✅ Найдена себестоимость: цена={}, остаток после списания={}",
                        entity.getPrice(), newQuantity);
                return entity.getPrice();
            } else {
                log.debug("⚠️ Недостаточно количества в приходнике: требуется {}, доступно {}",
                        quantity, invoiceQuantity);
            }
        }

        log.debug("❌ Себестоимость не найдена");
        return BigDecimal.ZERO;
    }

    private List<ExpendEntity> findAllExpend() {
        log.debug("🔍 Поиск всех расходных накладных в БД");
        return expendRepository.findAllByOrderByDateDesc();
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

    private void updateMapForInvoiceStocks() {
        log.debug("🔄 Корректировка приходников с учетом остатков");

        AtomicInteger processedNom = new AtomicInteger(0);
        AtomicInteger processedChar = new AtomicInteger(0);
        AtomicInteger processedBatch = new AtomicInteger(0);
        AtomicInteger adjustedRecords = new AtomicInteger(0);

        List<UUID> keysListNom = new ArrayList<>(this.invoiceStocksMap.keySet());

        for (UUID nomenclatureKey : keysListNom) {
            processedNom.incrementAndGet();

            if (!this.remainigStocksMap.containsKey(nomenclatureKey)) {
                log.debug("  - Номенклатура {}: нет в остатках", nomenclatureKey);
                continue;
            }

            List<UUID> keysListChar = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).keySet());

            for (UUID characteristicKey : keysListChar) {
                processedChar.incrementAndGet();

                if (!this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    log.debug("  - Характеристика {}: нет в остатках", characteristicKey);
                    continue;
                }

                List<UUID> keysListB = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).keySet());

                for (UUID batchKey : keysListB) {
                    processedBatch.incrementAndGet();

                    if (!this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                        log.debug("  - Партия {}: нет в остатках", batchKey);
                        continue;
                    }

                    List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    RemainingItemStockResponseDto remainingItem = this.remainigStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    BigDecimal remainingStockQuantity = remainingItem.getQuantityBalance().setScale(3, RoundingMode.HALF_UP);
                    BigDecimal originalRemaining = remainingStockQuantity;

                    for (int i = invoiceStocksList.size() - 1; i >= 0; i--) {
                        InvoiceStocksEntity entity = invoiceStocksList.get(i);
                        BigDecimal invoiceQuantity = entity.getQuantity();

                        if (invoiceQuantity.compareTo(remainingStockQuantity) <= 0) {
                            // Полностью списываем приходник
                            entity.setQuantity(BigDecimal.ZERO);
                            remainingStockQuantity = remainingStockQuantity.subtract(invoiceQuantity);
                            adjustedRecords.incrementAndGet();

                            log.debug("{}   - Полное списание: приходник {} ({}), остаток после: {}",
                                    nomenclatureKey, i, invoiceQuantity, remainingStockQuantity);
                        } else {
                            // Частичное списание
                            entity.setQuantity(invoiceQuantity.subtract(remainingStockQuantity));
                            remainingStockQuantity = BigDecimal.valueOf(0.0);
                            adjustedRecords.incrementAndGet();

                            log.debug("{}   - Частичное списание: приходник {} ({} -> {}), остаток обнулен",
                                    nomenclatureKey, i, invoiceQuantity, entity.getQuantity());
                            break;
                        }

                        invoiceStocksList.set(i, entity);
                    }

                    // Обновляем остаток
                    remainingItem.setQuantityBalance(remainingStockQuantity);
                    this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItem);

                    log.debug("  - Партия {}: скорректировано, остаток изменен с {} на {}",
                            batchKey, originalRemaining, remainingStockQuantity);
                }
            }
        }

        log.debug("✅ Корректировка завершена: обработано номенклатур={}, характеристик={}, партий={}, скорректировано записей={}",
                processedNom.get(), processedChar.get(), processedBatch.get(), adjustedRecords.get());
    }

    private void createMapForRemainigStocks(UUID organizationId, LocalDateTime endDate) {
        log.debug("🔍 Загрузка остатков из 1С: организация={}, дата={}", organizationId, endDate);

        this.remainigStocksMap.clear();

        RemainingStockResponseDto remainingStockResponseDto = getAllStocks(organizationId, endDate);
        List<RemainingItemStockResponseDto> entities = remainingStockResponseDto.getValue();

        log.debug("Получено {} записей остатков из 1С", entities.size());

        for (RemainingItemStockResponseDto entity : entities) {
            if (entity.getOrganizationKey().compareTo(organizationId) == 0) {
                Map<UUID, Map<UUID, RemainingItemStockResponseDto>> mapLevel2 = new HashMap<>();
                Map<UUID, RemainingItemStockResponseDto> mapLevel1 = new HashMap<>();

                UUID id_nom = entity.getNomenclatureKey();
                UUID id_char = entity.getCharacteristicKey();
                UUID id_b = entity.getBatchKey();

                mapLevel1.put(id_b, entity);
                mapLevel2.put(id_char, mapLevel1);
                this.remainigStocksMap.put(id_nom, mapLevel2);
            } else {
                log.debug("❌ {}<->{} Разные организации!!", entity.getOrganizationKey(),organizationId);
            }
        }

        log.debug("✅ Остатки загружены, уникальных номенклатур: {}", this.remainigStocksMap.size());
    }

    private void createMapForInvoiceStocks() {
        log.debug("🔍 Загрузка запасов приходных накладных");

        this.invoiceStocksMap.clear();

        String operationType = "ПоступлениеОтПоставщика";
        List<UUID> refKeys = invoiceRepository.findAllRefKeysByOperationType(operationType);

        if (refKeys.isEmpty()) {
            log.warn("⚠️ Не найдено приходных накладных с типом операции: {}", operationType);
            return;
        }

        log.debug("Найдено {} приходных накладных с типом операции '{}'", refKeys.size(), operationType);

        List<InvoiceStocksEntity> allStocks = invoiceStocksRepository.findAllByRefKeyIn(refKeys);

        if (allStocks.isEmpty()) {
            log.warn("⚠️ Не найдено запасов для приходных накладных");
            return;
        }

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

////        Сортируем по desc
//        // 1. Создаем Map: refKey -> Entity (для быстрого доступа O(1))
//        Map<UUID, InvoiceStocksEntity> stocksMap = new HashMap<>();
//        for (InvoiceStocksEntity stock : allStocks) {
//            stocksMap.put(stock.getRefKey(), stock); // Предполагаем, что есть метод getRefKey()
//        }
//
//        // 2. Формируем результат в нужном порядке
//        List<InvoiceStocksEntity> sortedStocks = new ArrayList<>();
//        for (UUID key : refKeys) {
//            InvoiceStocksEntity entity = stocksMap.get(key);
//            if (entity != null) { // Проверка на случай, если для какого-то ключа не нашлось записи
//                sortedStocks.add(entity);
//            }
//        }

        this.invoiceStocksMap = allStocks.stream()
                .collect(Collectors.groupingBy(
                        InvoiceStocksEntity::getNomenclatureKey,
                        Collectors.groupingBy(
                                InvoiceStocksEntity::getCharacteristicKey,
                                Collectors.groupingBy(
                                        InvoiceStocksEntity::getBatchKey,
                                        Collectors.toList()
                                )
                        )
                ));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.invoiceStocksMap.size());
    }

    private Map<UUID, List<ExpendStocksEntity>> createMapForExpendStocks(List<ExpendEntity> list) {
        log.debug("🔍 Загрузка запасов расходных накладных");

        if (list == null || list.isEmpty()) {
            log.debug("Список расходников пуст");
            return Collections.emptyMap();
        }

        List<UUID> refKeys = list.stream()
                .map(ExpendEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        log.debug("Получено {} уникальных refKey расходников", refKeys.size());

        List<ExpendStocksEntity> allExpendStocks = expendStocksRepository.findAllByRefKeyIn(refKeys);
        log.debug("Загружено {} записей запасов из БД", allExpendStocks.size());

        Map<UUID, List<ExpendStocksEntity>> dataMap = allExpendStocks.stream()
                .collect(Collectors.groupingBy(ExpendStocksEntity::getRefKey));

        List<UUID> missingRefKeys = refKeys.stream()
                .filter(key -> !dataMap.containsKey(key))
                .collect(Collectors.toList());

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} расходников без запасов, загружаем из 1С...", missingRefKeys.size());
            try {
                Map<UUID, List<ExpendStocksEntity>> foundMissing = expendStocksService
                        .findExpendStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    foundMissing.forEach((key, value) ->
                            dataMap.merge(key, value, (v1, v2) -> {
                                List<ExpendStocksEntity> merged = new ArrayList<>(v1);
                                merged.addAll(v2);
                                return merged;
                            })
                    );
                    int totalFound = foundMissing.values().stream().mapToInt(List::size).sum();
                    log.info("✅ Загружено {} записей из 1С для {} расходников", totalFound, foundMissing.size());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        log.debug("✅ Мапа расходников создана: {} документов с запасами", dataMap.size());
        return dataMap;
    }

    private RemainingStockResponseDto getAllStocks(
            UUID guid,
            LocalDateTime endDate
    ) {
        log.debug("📡 Запрос к 1С: получение остатков для организации {}", guid);

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Period=datetime'" + endDate + "'" +
                "Condition='cast(Организация_Key, 'Catalog_Организации') eq guid'%s'')" +
                "?" +
                "$select=Организация_Key,Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance, СуммаBalance&" +
                "$format=json", guid);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            long requestStart = System.currentTimeMillis();
            RemainingStockResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(RemainingStockResponseDto.class);

            log.debug("✅ Запрос выполнен за {} мс", System.currentTimeMillis() - requestStart);
            return response;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении остатков из 1С: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    private List<CostPriceControllerOutput> aggregateOnlyFast(List<CostPriceControllerOutput> products) {
        log.debug("🔄 Агрегация результатов");

        if (products == null || products.isEmpty()) {
            log.debug("Список продуктов пуст");
            return new ArrayList<>();
        }

        log.debug("Начальное количество записей: {}", products.size());

        Map<String, CostPriceControllerOutput> map = new HashMap<>(products.size());

        for (CostPriceControllerOutput p : products) {
            String key = p.getName() + "|" + p.getCharacteristic() + "|" + p.getBatch() + "|" + p.getCost();

            CostPriceControllerOutput existing = map.get(key);
            if (existing == null) {
                String productName = p.getName() != null ? p.getName() : "Без имени";
                String productCharacteristic = p.getCharacteristic() != null ? p.getCharacteristic() : "Без характеристики";
                String productBatch = p.getBatch() != null ? p.getBatch() : "Без партии";

                map.put(key, CostPriceControllerOutput.builder()
                        .refKey(p.getRefKey())
                        .number(p.getNumber())
                        .name(productName)
                        .characteristic(productCharacteristic)
                        .batch(productBatch)
                        .quantity(p.getQuantity())
                        .price(p.getPrice())
                        .cost(p.getCost())
                        .build()
                );
            } else {
                existing.setQuantity(existing.getQuantity().add(p.getQuantity()));
            }
        }

        List<CostPriceControllerOutput> result = new ArrayList<>(map.values());
        result.sort(Comparator.comparing(CostPriceControllerOutput::getName));

        log.debug("✅ Агрегация завершена: {} -> {} записей", products.size(), result.size());
        return result;
    }
}