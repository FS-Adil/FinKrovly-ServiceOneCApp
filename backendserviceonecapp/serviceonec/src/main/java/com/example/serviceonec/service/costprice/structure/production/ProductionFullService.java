package com.example.serviceonec.service.costprice.structure.production;

import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceFullEntity;
import com.example.serviceonec.model.entity.production.ProductionDistributionStocksEntity;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionFullEntity;
import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import com.example.serviceonec.repository.BatchRepository;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.invoice.InvoiceFullRepository;
import com.example.serviceonec.repository.production.ProductionDistributionStocksRepository;
import com.example.serviceonec.repository.production.ProductionFullRepository;
import com.example.serviceonec.repository.production.ProductionItemsRepository;
import com.example.serviceonec.repository.production.ProductionRepository;
import com.example.serviceonec.repository.remaining.RemainingRepository;
import com.example.serviceonec.service.production.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductionFullService {

    private final ProductionService productionService;

    private final ExpendRepository expendRepository;
    private final ProductionRepository productionRepository;
    private final ProductionItemsRepository productionItemsRepository;
    private final ProductionDistributionStocksRepository productionDistributionStocksRepository;
    private final ProductionFullRepository productionFullRepository;
    private final InvoiceFullRepository invoiceFullRepository;
    private final RemainingRepository remainingRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final BatchRepository batchRepository;

    private Map<UUID, Map<UUID, Map<UUID, List<InvoiceFullEntity>>>> invoiceFullMap = new HashMap<>();
    private Map<UUID, Map<UUID, Map<UUID, RemainingEntity>>> remainigStocksMap = new HashMap<>();
    private Map<UUID, String> nomenclatureMap = new HashMap<>();
    private Map<UUID, String> characteristicMap = new HashMap<>();
    private Map<UUID, String> batchMap = new HashMap<>();


    public void getAllProduction(
            UUID organizationId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        productionService.getAllProduction(
                organizationId,
                dateFrom,
                dateTo
        );
    }

    public void addAllProduction() {
        List<ExpendEntity> expendEntities = expendRepository.findAll();

        // Необходимо сравнить UUID расходников и производства,
        // для выявления недостающих производств привязанных к заказам покупателей
        if (expendEntities.isEmpty()) {
            log.debug("Список расходников пуст");
            return;
        }

        List<UUID> docOrders = expendEntities.stream()
                .map(ExpendEntity::getDocOrder)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.debug("Получено {} уникальных docOrder расходников", docOrders.size());

        List<ProductionEntity> productionEntities = productionRepository.findAll();
        if (productionEntities.isEmpty()) {
            log.debug("Список документов по производству пуст");
            return;
        }

        List<UUID> customerOrderKey = productionEntities.stream()
                .map(ProductionEntity::getCustomerOrderKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.debug("Получено {} уникальных customerOrderKey документов по производству", customerOrderKey.size());

        Set<UUID> customerOrderSet = new HashSet<>(customerOrderKey);
        List<UUID> missingCustomerOrderKeys = docOrders.stream()
                .filter(key -> !customerOrderSet.contains(key))
                .toList();
        if (!missingCustomerOrderKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} расходников без документов на производство, загружаем из 1С...", missingCustomerOrderKeys.size());
            try {

                productionService.getAllProductionByCustomerOrders(missingCustomerOrderKeys);

            } catch(Exception e){
                log.error("❌ Ошибка загрузки недостающих документов по производству: {}", e.getMessage(), e);
            }
        }
    }

    public void calculationCostPriceProduction() {
        // Получаем приходные накладные и остатки
        createMapForInvoiceStocks();
        createMapForRemainingStocks();
        // Обновляем приходные накладные исходя из остатков
        updateMapForInvoiceStocks();

        log.info("  - Загрузка номенклатуры...");
        this.nomenclatureMap = createMapForNomenclature();

        log.info("  - Загрузка характеристик...");
        this.characteristicMap = createMapForCharacteristic();

        log.info("  - Загрузка партий...");
        this.batchMap = createMapForBatch();
    }

    public void fillProductionFullEntity() {

        List<ProductionFullEntity> productionFullEntities = new ArrayList<>();
        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);

        // Получаем документы по производству
        List<ProductionEntity> productionEntityList = productionRepository.findAll();
        // Группируем по ref_key (предполагая, что refKey уникален)
        Map<UUID, ProductionEntity> productionEntityMap = productionEntityList.stream()
                .collect(Collectors.toMap(
                        ProductionEntity::getRefKey,  // ключ - refKey
                        Function.identity()           // значение - сам объект
                ));

        //Получаем производство ПРОДУКЦИИ
        List<ProductionItemsEntity> productionItemsEntityList = productionItemsRepository.findAll();

        // Получаем производство Распределение Запасов
        List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList = productionDistributionStocksRepository.findAll();
        // Групперуем по ref_key и ключ связи
        Map<UUID, Map<String, List<ProductionDistributionStocksEntity>>> productionDistributionStocksEntityMap = productionDistributionStocksEntityList.stream()
                .collect(Collectors.groupingBy(
                        ProductionDistributionStocksEntity::getRefKey,
                        Collectors.groupingBy(
                                ProductionDistributionStocksEntity::getProductLinkKey,
                                Collectors.toList()
                        )));

        AtomicInteger notFoundNomenclature = new AtomicInteger(0);
        AtomicInteger notFoundCharacteristic = new AtomicInteger(0);
        AtomicInteger notFoundBatch = new AtomicInteger(0);
        AtomicInteger zeroCost = new AtomicInteger(0);
        AtomicInteger foundWithCost = new AtomicInteger(0);

        // Расчитываем себестоимость для производства
        for (ProductionItemsEntity entity : productionItemsEntityList) {
            processedStocks.incrementAndGet();
            int productionNum = processedExpend.incrementAndGet();

            if (productionNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} документов на производство", productionNum, productionItemsEntityList.size());
            }

            UUID refKey = entity.getRefKey();
            String linkKey = entity.getLinkKey();

            ProductionEntity productionEntity = productionEntityMap.get(refKey);
            LocalDateTime date = productionEntity.getDate();
            String documentType = "ПРИХОД";
            String documentTypeOneC = "Производство";
            String number = productionEntity.getNumber();
            UUID organizationKey = productionEntity.getOrganizationKey();
            UUID structuralUnitKey = entity.getStructuralUnitKey();
            UUID customerOrder = productionEntity.getCustomerOrderKey();

            BigDecimal  quantity = entity.getQuantity();
            UUID nomenclatureKey = entity.getNomenclatureKey();
            UUID characteristicKey = entity.getCharacteristicKey();
            UUID batchKey = entity.getBatchKey();

            // Получаем map для refKey
            Map<String, List<ProductionDistributionStocksEntity>> refKeyMap = productionDistributionStocksEntityMap.get(refKey);

            if (refKeyMap == null) {
                log.error("❌ Не найдены записи для ref_key: {}", refKey);
                // Обработайте ситуацию - может быть, создать пустой список или продолжить без обработки
                addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                        batchKey, quantity, BigDecimal.valueOf(0.01)
                );
                continue; // или break, или return, в зависимости от контекста
            }

            // Получаем список для linkKey
            List<ProductionDistributionStocksEntity> stocksList = refKeyMap.get(linkKey);

            if (stocksList == null || stocksList.isEmpty()) {
                log.error("❌ Список материалов с ref_key {} и ключом {} пуст или не найден", refKey, linkKey);
                // Обработайте ситуацию
                continue;
            }

            if (stocksList.size() > 1) {
                log.error("❌ Список материалов с ref_key {} и ключом {} имеет больше 2 значений", refKey, linkKey);
                // Обработайте ситуацию
                continue;
            }

            ProductionDistributionStocksEntity stocks = productionDistributionStocksEntityMap
                    .get(refKey)
                    .get(entity.getLinkKey())
                    .getFirst();

            BigDecimal stocksQuantity = stocks.getQuantity();
            UUID stocksNomenclatureKey = stocks.getNomenclatureKey();
            UUID stocksCharacteristicKey = stocks.getCharacteristicKey();
            UUID stocksBatchKey = stocks.getBatchKey();

            String name = nomenclatureMap.getOrDefault(stocksNomenclatureKey, "Не найдено");
            String characteristic = characteristicMap.getOrDefault(stocksCharacteristicKey, "Не найдено");
            String batch = batchMap.getOrDefault(stocksBatchKey, "Не найдено");


            // Проверка наличия в приходниках
            if (!invoiceFullMap.containsKey(nomenclatureKey)) {
                notFoundNomenclature.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                        batchKey, quantity, BigDecimal.ZERO
                );
                continue;
            }
            if (!invoiceFullMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                notFoundCharacteristic.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                        batchKey, quantity, BigDecimal.ZERO
                );
                continue;
            }
            if (!invoiceFullMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                notFoundBatch.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                        batchKey, quantity, BigDecimal.ZERO
                );
                continue;
            }

            List<Map<String, BigDecimal>> listCost = getMapCostForNomenclature(
                    stocksNomenclatureKey,
                    name,
                    stocksCharacteristicKey,
                    characteristic,
                    stocksBatchKey,
                    batch,
                    stocksQuantity
            );

            for (Map<String, BigDecimal> map : listCost) {
                BigDecimal price = map.get("cost");
                quantity = map.get("quantity"); // нужно сопоставить количества материала к количеству продукции
                if (price.compareTo(BigDecimal.ZERO) == 0) {
                    zeroCost.incrementAndGet();
                    addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                            organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                            batchKey, quantity, BigDecimal.ZERO
                    );
                } else {
                    foundWithCost.incrementAndGet();
                    addToResult(productionFullEntities, date, documentType, documentTypeOneC, number, refKey,
                            organizationKey, structuralUnitKey, customerOrder, nomenclatureKey, characteristicKey,
                            batchKey, quantity, price
                    );
                }
            }
        }

        log.info("📦 Всего позиций продукции в документах на производства: {}", processedStocks.get());
        log.info("✅ Найдено с себестоимостью: {}", foundWithCost.get());
        log.info("❌ Не найдено номенклатуры для расчета себестоимости производства: {}", notFoundNomenclature.get());
        log.info("❌ Не найдено характеристик для расчета себестоимости производства: {}", notFoundCharacteristic.get());
        log.info("❌ Не найдено партий для расчета себестоимости производства: {}", notFoundBatch.get());
        log.info("❌ Нулевая себестоимость: {}", zeroCost.get());

        productionFullRepository.saveAll(productionFullEntities);
    }

    private void addToResult(List<ProductionFullEntity> list, LocalDateTime date, String documentType,
                             String documentTypeOneC, String number, UUID refKey, UUID organizationKey, UUID structuralUnitKey,
                             UUID customerOrder, UUID nomenclatureKey, UUID characteristicKey, UUID batchKey,
                             BigDecimal quantity, BigDecimal price) {
        list.add(ProductionFullEntity.builder()
                .date(date)
                        .documentType(documentType)
                        .documentTypeOneC(documentTypeOneC)
                        .number(number)
                        .refKey(refKey)
                        .organizationKey(organizationKey)
                        .structuralUnitKey(structuralUnitKey)
                        .customerOrder(customerOrder)
                        .nomenclatureKey(nomenclatureKey)
                        .characteristicKey(characteristicKey)
                        .batchKey(batchKey)
                        .quantity(quantity)
                        .price(price)
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
        log.debug("🔍 Расчет себестоимости для производства: номенклатура={}<->{}, характеристика={}<->{}, партия={}<->{}, количество={}",
                nomenclatureKey,
                name,
                characteristicKey,
                characteristic,
                batchKey,
                batch,
                quantity);

        List<InvoiceFullEntity> invoiceStocksList = this.invoiceFullMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        List<Map<String, BigDecimal>> list = new ArrayList<>();

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            HashMap<String, BigDecimal> map = new HashMap<>();

            InvoiceFullEntity entity = invoiceStocksList.get(i);

            BigDecimal invoiceQuantity = entity.getQuantity().setScale(3, RoundingMode.HALF_UP);

            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("❌ Количество не найдена цена={}", quantity);
                break;
            }
            if (invoiceQuantity.compareTo(quantity) >= 0) {
                BigDecimal newQuantity = invoiceQuantity.subtract(quantity).setScale(3, RoundingMode.HALF_UP);

                entity.setQuantity(newQuantity);

                invoiceStocksList.set(i, entity);
                this.invoiceFullMap
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
                this.invoiceFullMap
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

    private void createMapForInvoiceStocks() {

        this.invoiceFullMap.clear();

        List<InvoiceFullEntity> allInvoiceFullEntity = invoiceFullRepository.findAll();

        if (allInvoiceFullEntity.isEmpty()) {
            log.warn("⚠️ Не найдено данных в таблице invoice_full");
            return;
        }

        log.debug("Загружено {} записей запасов из БД", allInvoiceFullEntity.size());

        this.invoiceFullMap = allInvoiceFullEntity.stream()
                .collect(Collectors.groupingBy(
                        InvoiceFullEntity::getNomenclatureKey,
                        Collectors.groupingBy(
                                InvoiceFullEntity::getCharacteristicKey,
                                Collectors.groupingBy(
                                        InvoiceFullEntity::getBatchKey,
                                        Collectors.toList()
                                )
                        )
                ));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.invoiceFullMap.size());
    }

    private void createMapForRemainingStocks() {
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

    private void updateMapForInvoiceStocks() {
        log.debug("🔄 Корректировка приходников с учетом остатков");

        AtomicInteger processedNom = new AtomicInteger(0);
        AtomicInteger processedChar = new AtomicInteger(0);
        AtomicInteger processedBatch = new AtomicInteger(0);
        AtomicInteger adjustedRecords = new AtomicInteger(0);

        List<UUID> keysListNom = new ArrayList<>(this.invoiceFullMap.keySet());

        for (UUID nomenclatureKey : keysListNom) {
            processedNom.incrementAndGet();

            if (!this.remainigStocksMap.containsKey(nomenclatureKey)) {
                log.debug("  - Номенклатура {}: нет в остатках", nomenclatureKey);
                continue;
            }

            List<UUID> keysListChar = new ArrayList<>(this.invoiceFullMap.get(nomenclatureKey).keySet());

            for (UUID characteristicKey : keysListChar) {
                processedChar.incrementAndGet();

                if (!this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    log.debug("  - Характеристика {}: нет в остатках", characteristicKey);
                    continue;
                }

                List<UUID> keysListB = new ArrayList<>(this.invoiceFullMap.get(nomenclatureKey).get(characteristicKey).keySet());

                for (UUID batchKey : keysListB) {
                    processedBatch.incrementAndGet();

                    if (!this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                        log.debug("  - Партия {}: нет в остатках", batchKey);
                        continue;
                    }

                    List<InvoiceFullEntity> invoiceStocksList = this.invoiceFullMap
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
                        InvoiceFullEntity entity = invoiceStocksList.get(i);
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
                            this.invoiceFullMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
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

        log.debug("✅ Корректировка завершена: обработано номенклатур={}, характеристик={}, партий={}, скорректировано записей={}",
                processedNom.get(), processedChar.get(), processedBatch.get(), adjustedRecords.get());
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
