package com.example.serviceonec.service.costprice.structure.production;

import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.production.ProductionDistributionStocksEntity;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionFullEntity;
import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import com.example.serviceonec.model.entity.remaining.RemainingEntity;
import com.example.serviceonec.model.entity.resultingincome.ResultingIncomeFullEntity;
import com.example.serviceonec.model.entity.specification.SpecificationEntity;
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
import com.example.serviceonec.repository.resultingincome.ResultingIncomeFullRepository;
import com.example.serviceonec.repository.specification.SpecificationRepository;
import com.example.serviceonec.service.production.ProductionAdditiveService;
import com.example.serviceonec.service.production.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private final ProductionAdditiveService productionAdditiveService;

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
    private final SpecificationRepository specificationRepository;
    private final ResultingIncomeFullRepository resultingIncomeFullRepository;

    private Map<UUID, Map<UUID, Map<UUID, RemainingEntity>>> remainigStocksMap = new HashMap<>();
    private Map<UUID, BigDecimal> spcificationMap = new HashMap<>();
    private Map<UUID, String> nomenclatureMap = new HashMap<>();
    private Map<UUID, String> characteristicMap = new HashMap<>();
    private Map<UUID, String> batchMap = new HashMap<>();
    private Map<UUID, ProductionEntity> productionEntityMap = new HashMap<>();
    private Map<UUID, Map<String, ProductionItemsEntity>> productionItemsEntityMap = new HashMap<>();
    private Map<UUID, Map<String, List<ProductionDistributionStocksEntity>>> productionDistributionStocksEntityMap = new HashMap<>();

    private Map<UUID, Map<UUID, Map<UUID, List<ResultingIncomeFullEntity>>>> resultingIncomeFullMap = new HashMap<>();

    private List<ProductionFullEntity> productionFullEntities = new ArrayList<>();

    private AtomicInteger notFoundNomenclature;
    private AtomicInteger notFoundCharacteristic;
    private AtomicInteger notFoundBatch;
    private AtomicInteger zeroCost;
    private AtomicInteger foundWithCost;


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

        productionAdditiveService.getAllProduction(
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
        List<UUID> missingCustomerOrderKeys = new ArrayList<>();
        if (!productionEntities.isEmpty()) {
            List<UUID> customerOrderKey = productionEntities.stream()
                    .map(ProductionEntity::getCustomerOrderKey)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            log.debug("Получено {} уникальных customerOrderKey документов по производству", customerOrderKey.size());

            Set<UUID> customerOrderSet = new HashSet<>(customerOrderKey);

            missingCustomerOrderKeys = docOrders.stream()
                    .filter(key -> !customerOrderSet.contains(key))
                    .toList();
        } else {
            log.info("Список документов по производству пуст");
            missingCustomerOrderKeys = docOrders.stream().toList();
        }


        if (!missingCustomerOrderKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} расходников без документов на производство, загружаем из 1С...", missingCustomerOrderKeys.size());
            try {

                productionService.getAllProductionByCustomerOrders(missingCustomerOrderKeys);

            } catch(Exception e){
                log.error("❌ Ошибка загрузки недостающих документов по производству: {}", e.getMessage(), e);
            }
        }
    }

    public void addAllProductionOld() {
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
            log.info("Список документов по производству пуст");
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

    public void loadingDataForProduction() {
        log.info(" -> Расчет производства - Загрузка спецификации...");
        createMapForSpecification();
        log.info(" -> Расчет производства - Загрузка каталога номенклатуры...");
        this.nomenclatureMap = createMapForNomenclature();
        log.info(" -> Расчет производства - Загрузка каталога характеристик...");
        this.characteristicMap = createMapForCharacteristic();
        log.info("  -> Расчет производства - Загрузка каталога партий...");
        this.batchMap = createMapForBatch();
        log.info("  -> Расчет производства - Загрузка документов всех производств за период...");
        this.productionEntityMap = createMapForProductionEntity();
        log.info("  -> Расчет производства - Загрузка документов распределение запасов...");
        this.productionDistributionStocksEntityMap = createMapForProductionDistributionStocksEntity();
        log.info("  -> Расчет производства - Загрузка документов Продукции...");
        this.productionItemsEntityMap = createMapForProductionItemsEntity();
    }

    public void calculationStageOne() {

        log.info(" Этап 1 -> Расчет производства - Загрузка приходных накладных...");
        this.resultingIncomeFullMap = createAllResultingIncomeFullEntity();
        log.info(" Этап 1 -> Расчет производства - Загрузка остатков...");
        createMapForRemainingStocks();
        log.info(" Этап 1 -> Расчет производства - Обновляем приходные накладные исходя из остатков...");
        updateMapForResultingIncomeFull("Этап 1");

        // Очищаем список с результатами себестоимости производства
        this.productionFullEntities.clear();

        notFoundNomenclature = new AtomicInteger(0);
        notFoundCharacteristic = new AtomicInteger(0);
        notFoundBatch = new AtomicInteger(0);
        zeroCost = new AtomicInteger(0);
        foundWithCost = new AtomicInteger(0);

        //Получаем распределение запасов
        List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList =
                productionDistributionStocksRepository.findAll();

        // Создаем новый список для отфильтрованных элементов
        List<ProductionDistributionStocksEntity> filteredList = new ArrayList<>();

        for (ProductionDistributionStocksEntity entity : productionDistributionStocksEntityList) {
            if (entity.getBatchKey().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000000")) != 0) {
                filteredList.add(entity);
            }
        }

        calculation(filteredList);

        fillProductionFullEntity("Этап 1");
    }

    public void calculationStageTwo() {

        log.info(" Этап 2 -> Расчет производства - Загрузка приходных накладных...");
        this.resultingIncomeFullMap = createAllResultingIncomeFullEntity();
        log.info(" Этап 2 -> Расчет производства - Загрузка остатков...");
        createMapForRemainingStocks();
        log.info(" Этап 2 -> Расчет производства - Обновляем приходные накладные исходя из остатков...");
        updateMapForResultingIncomeFull("Этап 2");

        //Получаем распределение запасов
        List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList =
                productionDistributionStocksRepository.findAll();

        // Создаем новый список для отфильтрованных элементов
        List<ProductionDistributionStocksEntity> filteredList = new ArrayList<>();

        for (ProductionDistributionStocksEntity entity : productionDistributionStocksEntityList) {
            if (entity.getBatchKey().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000000")) == 0) {
                filteredList.add(entity);
            }
        }

        calculation(filteredList);

        fillProductionFullEntity("Этап 2");
    }

    public void calculationStageThree() {
        log.info(" Этап 3 -> Расчет производства - Загрузка приходных накладных...");
        this.resultingIncomeFullMap = createAllResultingIncomeFullEntity();
        log.info(" Этап 3 -> Расчет производства - Загрузка остатков...");
        createMapForRemainingStocks();
        log.info(" Этап 3 -> Расчет производства - Обновляем приходные накладные исходя из остатков...");
        updateMapForResultingIncomeFull("Этап 3");

        List<UUID> refKeys = this.productionFullEntities.stream()
                .filter(entity -> entity.getPrice() != null && entity.getPrice().compareTo(BigDecimal.ZERO) == 0)
                .map(ProductionFullEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (ProductionFullEntity productionFullEntities : this.productionFullEntities) {
            BigDecimal price = productionFullEntities.getPrice();

            if (price.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("❌ У данной номенклатуры нет себестоимости: {} ❌", nomenclatureMap.get(productionFullEntities.getNomenclatureKey()));
            }
        }

        //Получаем распределение запасов
        List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList =
                productionDistributionStocksRepository.findAllByRefKeyIn(refKeys);

        calculation(productionDistributionStocksEntityList);

        fillProductionFullEntity("Этап 3");
    }

    private void fillProductionFullEntity(String string) {
        productionFullRepository.deleteAll();
        log.info("{} - > ✓ Таблица production_full очищена", string);


        log.info("{} - > 📦 Всего позиций продукции в документах на производства: {}", string, this.productionItemsEntityMap.size());
        log.info("{} - > ✅ Найдено с себестоимостью: {}", string, foundWithCost.get());
        log.info("{} - > ❌ Не найдено номенклатуры для расчета себестоимости производства: {}", string,  notFoundNomenclature.get());
        log.info("{} - > ❌ Не найдено характеристик для расчета себестоимости производства: {}", string, notFoundCharacteristic.get());
        log.info("{} - > ❌ Не найдено партий для расчета себестоимости производства: {}", string, notFoundBatch.get());
        log.info("{} - > ❌ Нулевая себестоимость: {}", string, zeroCost.get());

        List<ProductionFullEntity> newEntities = new ArrayList<>();
        for (ProductionFullEntity entity : this.productionFullEntities) {
            ProductionFullEntity newEntity = new ProductionFullEntity();
            // Копируем все поля КРОМЕ ID
            BeanUtils.copyProperties(entity, newEntity, "id");
            newEntities.add(newEntity);
        }

        productionFullRepository.saveAll(newEntities);
    }

    private void addToResult(LocalDateTime date, String documentType,
                             String documentTypeOneC, String number, UUID refKey, UUID organizationKey, UUID structuralUnitKey,
                             UUID customerOrder, UUID nomenclatureKey, UUID characteristicKey, UUID batchKey,
                             BigDecimal quantity, BigDecimal price) {
        this.productionFullEntities.add(ProductionFullEntity.builder()
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
            BigDecimal quantity,
            BigDecimal quantitySpecification
    ) {
        log.debug("🔍 Расчет себестоимости для производства: номенклатура={}<->{}, характеристика={}<->{}, партия={}<->{}, количество={}",
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
                map.put("quantity", quantity.divide(quantitySpecification, 3, RoundingMode.HALF_UP));
                map.put("cost", entity.getPrice().multiply(quantitySpecification));
                list.add(map);
                return list;
            } else if (invoiceQuantity.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("⚠️ Недостаточно количества для расчета себестоимости в приходнике: требуется {}, доступно {}",
                        quantity, invoiceQuantity);
            } else {
                log.debug("⚠️ Недостаточно количества для расчета себестоимости в приходнике: требуется {}, доступно {}",
                        quantity, invoiceQuantity);

                quantity = quantity.subtract(invoiceQuantity);

                map.put("quantity", invoiceQuantity.divide(quantitySpecification, 3, RoundingMode.HALF_UP));
                map.put("cost", entity.getPrice().multiply(quantitySpecification));
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

            map.put("quantity", quantity.divide(quantitySpecification, 3, RoundingMode.HALF_UP));
            map.put("cost", BigDecimal.ZERO);
            list.add(map);
            log.debug("❌ Себестоимость не найдена");
        }
        return list;
    }

    private void createMapForSpecification() {

        this.spcificationMap.clear();

        List<SpecificationEntity> allSpecification = specificationRepository.findAll();

        if (allSpecification.isEmpty()) {
            log.warn("⚠️ Не найдено данных в таблице specification");
            return;
        }

        log.debug("Загружено {} записей спецификаций из БД", allSpecification.size());

        this.spcificationMap = allSpecification.stream()
                .collect(Collectors.toMap(
                        SpecificationEntity::getRefKey,  // ключ - ref_key
                        SpecificationEntity::getQuantity // значение - quantity
                ));

        log.debug("✅ Мапа спецификации создана: {} позиций", this.spcificationMap.size());
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

    private void updateMapForResultingIncomeFull(String s) {
        log.info("{} -> 🔄 Корректировка приходников с учетом остатков", s);

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

                    List<ResultingIncomeFullEntity> resultingIncomeFullEntityList = this.resultingIncomeFullMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    RemainingEntity remainingItem = this.remainigStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    BigDecimal remainingStockQuantity = remainingItem.getQuantityBalance().setScale(3, RoundingMode.HALF_UP);
                    BigDecimal originalRemaining = remainingStockQuantity;

                    for (int i = 0; i < resultingIncomeFullEntityList.size(); i++) {
                        ResultingIncomeFullEntity entity = resultingIncomeFullEntityList.get(i);
                        BigDecimal invoiceQuantity = entity.getQuantity();

                        if (invoiceQuantity.compareTo(remainingStockQuantity) <= 0) {
                            // Полностью списываем приходник
                            entity.setQuantity(BigDecimal.ZERO);
                            remainingStockQuantity = remainingStockQuantity.subtract(invoiceQuantity);
                            adjustedRecords.incrementAndGet();

                            resultingIncomeFullEntityList.set(i, entity);

                            log.debug("{}   - Полное списание: приходник {} ({}), остаток после: {}",
                                    nomenclatureKey, i, invoiceQuantity, remainingStockQuantity);
                        } else {
                            // Частичное списание
                            entity.setQuantity(invoiceQuantity.subtract(remainingStockQuantity));
                            remainingStockQuantity = BigDecimal.valueOf(0.0);
                            adjustedRecords.incrementAndGet();

                            log.debug("{}   - Частичное списание: приходник {} ({} -> {}), остаток обнулен",
                                    nomenclatureKey, i, invoiceQuantity, entity.getQuantity());

                            resultingIncomeFullEntityList.set(i, entity);

                            //Обновляем приходники
                            this.resultingIncomeFullMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, resultingIncomeFullEntityList);
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

        log.info("{} -> ✅ Корректировка завершена: обработано номенклатур={}, характеристик={}, партий={}, скорректировано записей={}",
                s,
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

    private Map<UUID, ProductionEntity> createMapForProductionEntity() {
        this.productionEntityMap.clear();
        // Получаем документы по производству
        List<ProductionEntity> productionEntityList = productionRepository.findAll();
        // Группируем по ref_key (предполагая, что refKey уникален)
        return productionEntityList.stream()
                .collect(Collectors.toMap(
                        ProductionEntity::getRefKey,  // ключ - refKey
                        Function.identity()           // значение - сам объект
                ));
    }

    private Map<UUID, Map<String, List<ProductionDistributionStocksEntity>>> createMapForProductionDistributionStocksEntity() {
        this.productionDistributionStocksEntityMap.clear();
        // Получаем производство Распределение Запасов
        List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList = productionDistributionStocksRepository.findAll();
        // Групперуем по ref_key и ключ связи
        return productionDistributionStocksEntityList.stream()
                .collect(Collectors.groupingBy(
                        ProductionDistributionStocksEntity::getRefKey,
                        Collectors.groupingBy(
                                ProductionDistributionStocksEntity::getProductLinkKey,
                                Collectors.toList()
                        )));
    }

    private Map<UUID, Map<String, ProductionItemsEntity>> createMapForProductionItemsEntity() {
        this.productionItemsEntityMap.clear();
        // Получаем документы по производству
        List<ProductionItemsEntity> productionItemsEntityList = productionItemsRepository.findAll();
        // Групперуем по ref_key и ключ связи
        return productionItemsEntityList.stream()
                .collect(Collectors.groupingBy(
                        ProductionItemsEntity::getRefKey,
                        Collectors.toMap(
                                ProductionItemsEntity::getLinkKey,
                                Function.identity()
                        )));
    }

    public Map<UUID, Map<UUID, Map<UUID, List<ResultingIncomeFullEntity>>>> createAllResultingIncomeFullEntity() {
        log.debug("🔍 Загрузка всех приходных накладных и производств");

        this.resultingIncomeFullMap.clear();

        List<ResultingIncomeFullEntity> resultingIncomeFullEntityList = resultingIncomeFullRepository.findAllByOrderByDateDesc();

        if (resultingIncomeFullEntityList.isEmpty()) {
            log.warn("⚠️ Не найдено данных в таблице resulting_income_full");
            return new HashMap<>();
        }

        log.debug("Загружено {} записей запасов из БД", resultingIncomeFullEntityList.size());

        Map<UUID, Map<UUID, Map<UUID, List<ResultingIncomeFullEntity>>>> result;

        result =  resultingIncomeFullEntityList.stream()
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

        log.debug("✅ Мапа ПРИХОДОВ создана: {} номенклатур", result.size());

        return result;
    }

    private <T> void calculation(
            List<ProductionDistributionStocksEntity> productionDistributionStocksEntityList
    ) {

        for (ProductionDistributionStocksEntity entity : productionDistributionStocksEntityList) {

            UUID refKey = entity.getRefKey();
            String linkKey = entity.getProductLinkKey();

            ProductionEntity productionEntity = this.productionEntityMap.get(refKey);
            LocalDateTime date = productionEntity.getDate();
            String documentType = "ПРИХОД";
            String documentTypeOneC = "Производство";
            String number = productionEntity.getNumber();
            UUID organizationKey = productionEntity.getOrganizationKey();
            UUID structuralUnitKey = entity.getStructuralUnitKey();
            UUID customerOrder = productionEntity.getCustomerOrderKey();

            ProductionItemsEntity productionItemsEntity = this.productionItemsEntityMap.get(refKey).get(linkKey);
            BigDecimal  itemQuantity = productionItemsEntity.getQuantity();
            UUID itemNomenclatureKey = productionItemsEntity.getNomenclatureKey();
            UUID itemCharacteristicKey = productionItemsEntity.getCharacteristicKey();
            UUID itemBatchKey = productionItemsEntity.getBatchKey();
            UUID itemSpecification = productionItemsEntity.getSpecificationKey();
            BigDecimal quantitySpecification = this.spcificationMap.get(itemSpecification);

            String itemName = nomenclatureMap.getOrDefault(itemNomenclatureKey, "Продукция Не найдено");
            String itemCharacteristic = characteristicMap.getOrDefault(itemCharacteristicKey, "Продукция Не найдено");
            String itemBatch = batchMap.getOrDefault(itemBatchKey, "Продукция Не найдено");

            // Добавляем проверку на null для quantitySpecification
            if (quantitySpecification == null) {
                log.debug("❌ quantitySpecification = null для номенклатуры: {} <-> со спецификацией {}", itemName, itemSpecification);
                quantitySpecification = BigDecimal.ONE; // или другое значение по умолчанию
                // или выбросить исключение, если это критично
                // throw new IllegalArgumentException("quantitySpecification cannot be null");
            }

            // Получаем map для refKey
            Map<String, List<ProductionDistributionStocksEntity>> refKeyMap = this.productionDistributionStocksEntityMap.get(refKey);

            // Получаем список для linkKey
            List<ProductionDistributionStocksEntity> stocksList = refKeyMap.get(linkKey);

            if (stocksList == null || stocksList.isEmpty()) {
                log.error("❌ Список материалов с ref_key {} и ключом {} пуст или не найден", refKey, linkKey);
                // Обработайте ситуацию
//                continue;
            }

            if (stocksList.size() > 1) {
                log.debug("❌ Список материалов с ref_key {} и ключом {} имеет больше 2 значений", refKey, linkKey);
                // Обработайте ситуацию
//                continue;
            }

            BigDecimal stocksQuantity = entity.getQuantity();
            UUID stocksNomenclatureKey = entity.getNomenclatureKey();
            UUID stocksCharacteristicKey = entity.getCharacteristicKey();
            UUID stocksBatchKey = entity.getBatchKey();

            String stocksName = this.nomenclatureMap.getOrDefault(stocksNomenclatureKey, "Запасы Не найдено");
            String stockCharacteristic = this.characteristicMap.getOrDefault(stocksCharacteristicKey, "Запасы Не найдено");
            String stockBatch = this.batchMap.getOrDefault(stocksBatchKey, "Запасы Не найдено");

            // Проверка наличия в приходниках
            if (!resultingIncomeFullMap.containsKey(stocksNomenclatureKey)) {
                notFoundNomenclature.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, itemNomenclatureKey, itemCharacteristicKey,
                        itemBatchKey, itemQuantity, BigDecimal.ZERO
                );
                continue;
            }
            if (!resultingIncomeFullMap.get(stocksNomenclatureKey).containsKey(stocksCharacteristicKey)) {
                notFoundCharacteristic.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, itemNomenclatureKey, itemCharacteristicKey,
                        itemBatchKey, itemQuantity, BigDecimal.ZERO
                );
                continue;
            }
            if (!resultingIncomeFullMap.get(stocksNomenclatureKey).get(stocksCharacteristicKey).containsKey(stocksBatchKey)) {
                notFoundBatch.incrementAndGet();
                zeroCost.incrementAndGet();
                addToResult(date, documentType, documentTypeOneC, number, refKey,
                        organizationKey, structuralUnitKey, customerOrder, itemNomenclatureKey, itemCharacteristicKey,
                        itemBatchKey, itemQuantity, BigDecimal.ZERO
                );
                continue;
            }

            List<Map<String, BigDecimal>> listCost = getMapCostForNomenclature(
                    stocksNomenclatureKey,
                    stocksName,
                    stocksCharacteristicKey,
                    stockCharacteristic,
                    stocksBatchKey,
                    stockBatch,
                    stocksQuantity,
                    quantitySpecification
            );

            for (Map<String, BigDecimal> map : listCost) {
                BigDecimal price = map.get("cost");
                itemQuantity = map.get("quantity"); // нужно сопоставить количества материала к количеству продукции
                if (price.compareTo(BigDecimal.ZERO) == 0) {
                    zeroCost.incrementAndGet();
                    addToResult(date, documentType, documentTypeOneC, number, refKey,
                            organizationKey, structuralUnitKey, customerOrder, itemNomenclatureKey, itemCharacteristicKey,
                            itemBatchKey, itemQuantity, BigDecimal.ZERO
                    );
                } else {
                    foundWithCost.incrementAndGet();
                    addToResult(date, documentType, documentTypeOneC, number, refKey,
                            organizationKey, structuralUnitKey, customerOrder, itemNomenclatureKey, itemCharacteristicKey,
                            itemBatchKey, itemQuantity, price
                    );
                }
            }
        }
    }
}
