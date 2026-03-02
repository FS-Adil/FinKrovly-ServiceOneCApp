package com.example.serviceonec.service.costprice;

import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.model.dto.response.costprice.RemainingItemStockResponseDto;
import com.example.serviceonec.model.dto.response.costprice.RemainingStockResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListResponseDto;
import com.example.serviceonec.model.entity.BaseEntity;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.inventory.InventoryEntity;
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
import java.time.LocalDateTime;
import java.util.*;
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
        log.info("Start--------> CostPriceServiceImpl --------> getAllCostPrice");
        List<CostPriceControllerOutput> list = new ArrayList<>();

        List<ExpendEntity> expendList = findAllExpend(); // Находим все рассходники из бд
        Map<UUID, List<ExpendStocksEntity>> expendStocksMap = createMapForExpendStocks(expendList); // Находим все запасы расходников из бд
        createMapForInvoiceStocks(); // this.invoiceStocksMap создаем на основе данных из бд
        createMapForRemainigStocks(organizationId, endDate); // this.remainigStocksMap создаем структуру для хранения остатков товаров по id организации
        updateMapForInvoiceStocks(); // this.invoiceStocksMap изменяем исходя из остатков на складе, то есть убираем из структуры количество, которое числится в остатке

        Map<UUID, String> nomenclatureMap = createMapForNomenclature();
        Map<UUID, String> characteristicMap = createMapForCharacteristic();
        Map<UUID, String> batchMap = createMapForBatch();


        for (ExpendEntity expend : expendList) {
//            log.info("for ---> ExpendEntity expend : expendList");
            UUID expendRefKey = expend.getRefKey();

            if (expendStocksMap.get(expendRefKey) == null) {
                log.info("Запасы расходников по данному ref_key {} нет", expendRefKey);
                continue;
            }


            for (ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)) {
//                log.info("for -----> ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)");
                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                UUID characteristicKey = expendStocksEntity.getCharacteristicKey();
                UUID batchKey = expendStocksEntity.getBatchKey();

                String number = expend.getNumber();
                String refKey = expendRefKey.toString();
                String name = nomenclatureMap.get(nomenclatureKey);
                String characteristic = characteristicMap.get(characteristicKey);
                String batch = batchMap.get(batchKey);
                BigDecimal price = expendStocksEntity.getPrice();
                BigDecimal quantity = expendStocksEntity.getQuantity();

                if (!invoiceStocksMap.containsKey(nomenclatureKey)) {
//                    log.info("В приходниках такой номенклатуры нет --> {}", nomenclatureKey);
                    list.add(
                            CostPriceControllerOutput.builder()
                                    .refKey(refKey)
                                    .number(number)
                                    .name(name)
                                    .characteristic(characteristic)
                                    .batch(batch)
                                    .quantity(quantity)
                                    .price(BigDecimal.valueOf(0.0))
                                    .cost(BigDecimal.valueOf(0.0))
                                    .build()
                    );
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
//                    log.info("В приходниках такой характеристики нет --> {}-->{}", nomenclatureKey, characteristicKey);
                    list.add(
                            CostPriceControllerOutput.builder()
                                    .refKey(refKey)
                                    .number(number)
                                    .name(name)
                                    .characteristic(characteristic)
                                    .batch(batch)
                                    .quantity(quantity)
                                    .price(BigDecimal.valueOf(0.0))
                                    .cost(BigDecimal.valueOf(0.0))
                                    .build()
                    );
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
//                    log.info("В приходниках такой партии нет --> {}-->{}-->{}", nomenclatureKey, characteristicKey, batchKey);
                    list.add(
                            CostPriceControllerOutput.builder()
                                    .refKey(refKey)
                                    .number(number)
                                    .name(name)
                                    .characteristic(characteristic)
                                    .batch(batch)
                                    .quantity(quantity)
                                    .price(BigDecimal.valueOf(0.0))
                                    .cost(BigDecimal.valueOf(0.0))
                                    .build()
                    );
                    continue;
                }

                BigDecimal cost = getCostForNomenclature(
                        nomenclatureKey,
                        characteristicKey,
                        batchKey,
                        quantity
                );
                if (cost.compareTo(BigDecimal.valueOf(0.0)) == 0) {
//                    log.info("В приходнике количество меньше чем в расходнике - {}", nomenclatureKey);
                    list.add(
                            CostPriceControllerOutput.builder()
                                    .refKey(refKey)
                                    .number(number)
                                    .name(name)
                                    .characteristic(characteristic)
                                    .batch(batch)
                                    .quantity(quantity)
                                    .price(price)
                                    .cost(BigDecimal.valueOf(0.0))
                                    .build()
                    );
                    continue;
                }
//                log.info("{}-{}-{}-{}", name, price, quantity, cost);
                list.add(
                        CostPriceControllerOutput.builder()
                                .refKey(refKey)
                                .number(number)
                                .name(name)
                                .characteristic(characteristic)
                                .batch(batch)
                                .quantity(quantity)
                                .price(price)
                                .cost(cost)
                                .build()
                );

//                log.info(list.toString());
            }
        }
        log.info("Finish--------> CostPriceServiceImpl --------> getAllCostPrice");
        return aggregateOnlyFast(list);
    }

    private BigDecimal getCostForNomenclature(
            UUID nomenclatureKey,
            UUID characteristicKey,
            UUID batchKey,
            BigDecimal quantity
    ) {
//        log.info("Start--------> CostPriceServiceImpl --------> getCostForNomenclature");

        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            BigDecimal invoiceQuantity = entity.getQuantity();

            if ( invoiceQuantity.compareTo(quantity) >= 0) {
//                log.info("до ------>{}------>{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                entity.setQuantity(invoiceQuantity.subtract(quantity));
                invoiceStocksList.set(i, entity);
//                log.info("после ------>{}------->{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                this.invoiceStocksMap
                        .get(nomenclatureKey)
                        .get(characteristicKey)
                        .replace(batchKey, invoiceStocksList);

//                log.info("Finish--------> CostPriceServiceImpl --------> getCostForNomenclature");
                return entity.getPrice();
            } else {
                log.info("Вход в метод getCostForNomenclature с данным количеством ------>{}---->{}---->{}", quantity, invoiceQuantity, nomenclatureKey);
            }
        }
        return BigDecimal.valueOf(0.0);
    }

    private List<ExpendEntity> findAllExpend() {
        log.info("--------> CostPriceServiceImpl --------> findAllExpend");
        return expendRepository.findAll();
    }

    private List<InvoiceEntity> findAllInvoice() {
        log.info("--------> CostPriceServiceImpl --------> findAllInvoice");
        return invoiceRepository.findAll();
    }

    private Map<UUID, String> createMapForNomenclature() {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForNomenclature");
        List<NomenclatureEntity> entities = nomenclatureRepository.findAll();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (NomenclatureEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForNomenclature");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private Map<UUID, String> createMapForCharacteristic() {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForCharacteristic");
        List<CharacteristicEntity> entities = characteristicRepository.findAll();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (CharacteristicEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForCharacteristic");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private Map<UUID, String> createMapForBatch() {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForBatch");
        List<BatchEntity> entities = batchRepository.findAll();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, String> dataMap = new HashMap<>(initialCapacity);

        for (BatchEntity entity : entities) {
            dataMap.put(entity.getRefKey(), entity.getDescription());
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForBatch");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private void updateMapForInvoiceStocks() {
        log.info("Start --------> CostPriceServiceImpl --------> updateMapForInvoiceStocks");
        List<UUID> keysListNom = new ArrayList<>(this.invoiceStocksMap.keySet());

        for (UUID nomenclatureKey : keysListNom) {
            if (!this.remainigStocksMap.containsKey(nomenclatureKey)) {
//                log.info("На складах организации такой {} номенклатуры нет", nomenclatureKey);
                continue;
            }

//            log.info("for (UUID nomenclatureKey : keysListNom) {");
            List<UUID> keysListChar = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).keySet());

            for (UUID characteristicKey : keysListChar) {
                if (!this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
//                    log.info("На складах организации номенклатуры {} с такой {} характеристикой нет", nomenclatureKey, characteristicKey);
                    continue;
                }

//                log.info("for (UUID characteristicKey : keysListChar) {");
                List<UUID> keysListB = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).keySet());

                for (UUID batchKey : keysListB) {
                    if (!this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
//                        log.info("На складах организации такой номенклатуры {} с такой характеристикой {} с такой партией {} нет", nomenclatureKey, characteristicKey, batchKey);
                        continue;
                    }

//                    log.info("for (UUID batchKey : keysListB) {");
                    List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    RemainingItemStockResponseDto remainingItemStockResponseDto = this.remainigStocksMap
                            .get(nomenclatureKey)
                            .get(characteristicKey)
                            .get(batchKey);

                    double remainingStockQuantity = remainingItemStockResponseDto.getQuantityBalance();

                    for (int i = 0; i < invoiceStocksList.size(); i++) {
                        InvoiceStocksEntity entity = invoiceStocksList.get(i);
                        double invoiceQuantity = entity.getQuantity().doubleValue();

                        // Если количество в приходниках меньше, чем в остатках
                        if (invoiceQuantity <= remainingStockQuantity) {
//                            log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                            // Изменяем количество в приходниках на 0.0
                            entity.setQuantity(new BigDecimal(0.0));
                            invoiceStocksList.set(i, entity);
                            this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
                            // Уменьшаем количестов в остатках на количество в приходниках, чтоб не было повторного списания из следующих приходников
                            remainingStockQuantity = remainingStockQuantity - invoiceQuantity;
                            remainingItemStockResponseDto.setQuantityBalance(remainingStockQuantity);
                            this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItemStockResponseDto);
//                            log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                        } else {
//                            log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                            // Уменьшаем количество в приходниках на количества в остатках
                            entity.setQuantity(new BigDecimal(invoiceQuantity - remainingStockQuantity));
                            invoiceStocksList.set(i, entity);
                            this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
                            // Изменить количество в остатках на 0.0
                            remainingStockQuantity = 0.0;
                            remainingItemStockResponseDto.setQuantityBalance(remainingStockQuantity);
                            this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItemStockResponseDto);
//                            log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                            break;
                        }
                    }
                }
            }
        }
        log.info("Finish --------> CostPriceServiceImpl --------> updateMapForInvoiceStocks");
    }

    private void createMapForRemainigStocks(UUID organizationId, LocalDateTime endDate) {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForRemainigStocks");
        this.remainigStocksMap.clear();

        RemainingStockResponseDto remainingStockResponseDto = getAllStocks(organizationId, endDate);
        List<RemainingItemStockResponseDto> entities = remainingStockResponseDto.getValue();

        for (RemainingItemStockResponseDto entity : entities) {
            Map<UUID, Map<UUID, RemainingItemStockResponseDto>> mapLevel2 = new HashMap<>();
            Map<UUID, RemainingItemStockResponseDto> mapLevel1 = new HashMap<>();

            UUID id_nom = entity.getNomenclatureKey();
            UUID id_char = entity.getCharacteristicKey();
            UUID id_b = entity.getBatchKey();

            mapLevel1.put(id_b, entity);
            mapLevel2.put(id_char, mapLevel1);
            this.remainigStocksMap.put(id_nom, mapLevel2);
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForRemainigStocks");
        log.info("{}--{}", this.remainigStocksMap.size(), this.remainigStocksMap.hashCode());
    }

    private void createMapForInvoiceStocks() {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForInvoiceStocks");
        // Очищаем существующую мапу
        this.invoiceStocksMap.clear();

        // 1. Получаем все RefKey одним запросом
//        List<UUID> refKeys = invoiceRepository.findAllRefKeys();
//        if (refKeys.isEmpty()) {
//            log.info("No invoices found");
//            return;
//        }
        // Получаем только refKey для документов с operationType = "ПоступлениеОтПоставщика"
        String operationType = "ПоступлениеОтПоставщика"; // или используйте enum
        List<UUID> refKeys = invoiceRepository.findAllRefKeysByOperationType(operationType);

        if (refKeys.isEmpty()) {
            log.info("No invoices found with operation type: {}", operationType);
            return;
        }

        // 2. Загружаем все InvoiceStocks одним запросом
        List<InvoiceStocksEntity> allStocks = invoiceStocksRepository.findAllByRefKeyIn(refKeys);
        if (allStocks.isEmpty()) {
            log.info("No invoice stocks found for the filtered invoices");
            return;
        }

        // 3. Используем Java 8+ Stream API для эффективной группировки
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

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForInvoiceStocks");
        log.info("Found {} invoice stocks for {} invoices with operation type: {}",
                allStocks.size(), refKeys.size(), operationType);
    }

    private Map<UUID, List<ExpendStocksEntity>> createMapForExpendStocks(List<ExpendEntity> list) {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForExpendStocks");

        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        // Собираем все refKey для одного запроса
        List<UUID> refKeys = list.stream()
                .map(ExpendEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Получаем все записи одним запросом
        List<ExpendStocksEntity> allExpendStocks = expendStocksRepository.findAllByRefKeyIn(refKeys);

        // Группируем по refKey
        Map<UUID, List<ExpendStocksEntity>> dataMap = allExpendStocks.stream()
                .collect(Collectors.groupingBy(ExpendStocksEntity::getRefKey));

        // Проверяем, для каких refKeys нет записей
        List<UUID> missingRefKeys = refKeys.stream()
                .filter(key -> !dataMap.containsKey(key))
                .collect(Collectors.toList());

        if (!missingRefKeys.isEmpty()) {
            // Единоразово пытаемся загрузить недостающие записи
            try {
                Map<UUID, List<ExpendStocksEntity>> foundMissing = expendStocksService
                        .findExpendStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    // Объединяем с существующей мапой
                    foundMissing.forEach((key, value) ->
                            dataMap.merge(key, value, (v1, v2) -> {
                                List<ExpendStocksEntity> merged = new ArrayList<>(v1);
                                merged.addAll(v2);
                                return merged;
                            })
                    );
                }
            } catch (Exception e) {
                log.error("Error loading missing expend stocks: {}", e.getMessage(), e);
            }
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForExpendStocks");
        return dataMap;
    }

    private RemainingStockResponseDto getAllStocks(
            UUID guid,
            LocalDateTime endDate
    ) {
        log.info("------> CostPriceServiceImpl -------> getAllStocks");

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
                "Period=datetime'" + endDate + "'" +
                "Condition='cast(Организация_Key, 'Catalog_Организации') eq guid'%s'')" +
                "?" +
                "$select=Номенклатура_Key, Характеристика_Key, Партия_Key, КоличествоBalance, СуммаBalance&" +
                "$format=json", guid);

        RemainingStockResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(RemainingStockResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Списка остатков по организации %s", oneCProperties.getOneCGuidOpen()), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех остатков по организации {}", guid);

        return response;
    }

    // Более быстрая версия с строковым ключом
    private List<CostPriceControllerOutput> aggregateOnlyFast(List<CostPriceControllerOutput> products) {
        log.info("Start ------> CostPriceServiceImpl -------> aggregateOnlyFast");
        if (products == null || products.isEmpty()) {
            log.info("Finish ------> CostPriceServiceImpl -------> aggregateOnlyFast----> List is Empty");
            return new ArrayList<>();
        }

        Map<String, CostPriceControllerOutput> map = new HashMap<>(products.size());

        for (CostPriceControllerOutput p : products) {
            // Создаем уникальный ключ из имени и себестоимости
            String key = p.getName() + "|" + p.getCharacteristic() + "|" + p.getBatch() + "|" + p.getCost();

            CostPriceControllerOutput existing = map.get(key);
            if (existing == null) {
                // Создаем новый продукт
                // Создаем новый продукт с дефолтным значением для name, если оно null
                String productName = p.getName() != null ? p.getName() : "Без имени"; // или "Без имени"
                String productCharacteristic = p.getCharacteristic() != null ? p.getCharacteristic() : "Без характеритики";
                String productBatch = p.getBatch() != null ? p.getBatch() : "Без партии";
                map.put(key,
                        CostPriceControllerOutput.builder()
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
                // Суммируем количество
                existing.setQuantity(existing.getQuantity().add(p.getQuantity()));
            }
        }

        // Получаем список результатов и сортируем по возрастанию наименования
        List<CostPriceControllerOutput> result = new ArrayList<>(map.values());
        result.sort(Comparator.comparing(CostPriceControllerOutput::getName));

        log.info("Finish ------> CostPriceServiceImpl -------> aggregateOnlyFast. Sorted by quantity desc");
        return result;
    }
}
