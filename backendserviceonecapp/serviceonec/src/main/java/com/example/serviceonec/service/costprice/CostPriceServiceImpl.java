package com.example.serviceonec.service.costprice;

import com.example.serviceonec.config.OneCProperties;
import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.model.dto.response.costprice.RemainingItemStockResponseDto;
import com.example.serviceonec.model.dto.response.costprice.RemainingStockResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.dto.response.rolllist.RollListResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostPriceServiceImpl implements CostPriceService {

    private final Map<UUID, Map<UUID, Map<UUID, List<InvoiceStocksEntity>>>> invoiceStocksMap = new HashMap<>();
    private final Map<UUID, Map<UUID, Map<UUID, RemainingItemStockResponseDto>>> remainigStocksMap = new HashMap<>();

    private final ExpendRepository expendRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final NomenclatureRepository nomenclatureRepository;

    private final OneCProperties oneCProperties;
    private final RestClientConfig restClientConfig;

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


        for (ExpendEntity expend : expendList) {
            log.info("for ---> ExpendEntity expend : expendList");
            UUID expendRefKey = expend.getRefKey();

            if (expendStocksMap.get(expendRefKey) == null) {
                log.info("if (expendStocksMap.get(expendRefKey) == null) {");
                continue;
            }

            log.info("expendList ---- {} ------->{}--------->{}",
                    expendRefKey,
                    expendStocksMap.size(),
                    expendStocksMap.get(expendRefKey).size()
            );

            for (ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)) {
                log.info("for -----> ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)");
                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                UUID characteristicKey = expendStocksEntity.getCharacteristicKey();
                UUID batchKey = expendStocksEntity.getBatchKey();
                if (!invoiceStocksMap.containsKey(nomenclatureKey)) {
                    log.info("В приходниках такой номенклатуры нет --> {}", nomenclatureKey);
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    log.info("В приходниках такой характеристики нет --> {}-->{}", nomenclatureKey, characteristicKey);
                    continue;
                }
                if (!invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                    log.info("В приходниках такой партии нет --> {}-->{}-->{}", nomenclatureKey, characteristicKey, batchKey);
                    continue;
                }

                String name = nomenclatureMap.get(nomenclatureKey);
                double price = expendStocksEntity.getPrice().doubleValue();
                double quantity = expendStocksEntity.getQuantity().doubleValue();

                double cost = getCostForNomenclature(
                        nomenclatureKey,
                        characteristicKey,
                        batchKey,
                        quantity
                );
                if (cost == 0.0) {
                    log.info("В приходнике количество меньше чем в расходнике - {}", nomenclatureKey);
                    continue;
                }
//                log.info("{}-{}-{}-{}", name, price, quantity, cost);
                list.add(
                        CostPriceControllerOutput.builder()
                                .name(name)
                                .quantity(quantity)
                                .price(price)
                                .cost(cost)
                                .build()
                );

//                log.info(list.toString());
            }
        }
        log.info("Finish--------> CostPriceServiceImpl --------> getAllCostPrice");
        return list;
    }

    private double getCostForNomenclature(
            UUID nomenclatureKey,
            UUID characteristicKey,
            UUID batchKey,
            double quantity
    ) {
        log.info("Start--------> CostPriceServiceImpl --------> getCostForNomenclature");

        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap
                .get(nomenclatureKey)
                .get(characteristicKey)
                .get(batchKey);

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            double invoiceQuantity = entity.getQuantity().doubleValue();

            if ( invoiceQuantity >= quantity) {
                log.info("до ------>{}------>{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                entity.setQuantity(new BigDecimal(invoiceQuantity - quantity));
                invoiceStocksList.set(i, entity);
                log.info("после ------>{}------->{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                this.invoiceStocksMap
                        .get(nomenclatureKey)
                        .get(characteristicKey)
                        .replace(batchKey, invoiceStocksList);

                log.info("Finish--------> CostPriceServiceImpl --------> getCostForNomenclature");
                return entity.getPrice().doubleValue();
            }
        }
        log.info("Вход в метод getCostForNomenclature с данным количеством ------>{}------>{}", quantity, nomenclatureKey);
        return 0.0;
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

    private void updateMapForInvoiceStocks() {
        log.info("Start --------> CostPriceServiceImpl --------> updateMapForInvoiceStocks");
        List<UUID> keysListNom = new ArrayList<>(this.invoiceStocksMap.keySet());

        for (UUID nomenclatureKey : keysListNom) {
            if (!this.remainigStocksMap.containsKey(nomenclatureKey)) {
                log.info("if (this.remainigStocksMap.containsKey(nomenclatureKey)) {");
                continue;
            }

            log.info("for (UUID nomenclatureKey : keysListNom) {");
            List<UUID> keysListChar = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).keySet());

            for (UUID characteristicKey : keysListChar) {
                if (!this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {
                    log.info("if (this.remainigStocksMap.get(nomenclatureKey).containsKey(characteristicKey)) {");
                    continue;
                }

                log.info("for (UUID characteristicKey : keysListChar) {");
                List<UUID> keysListB = new ArrayList<>(this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).keySet());

                for (UUID batchKey : keysListB) {
                    if (!this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {
                        log.info("if (this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).containsKey(batchKey)) {");
                        continue;
                    }

                    log.info("for (UUID batchKey : keysListB) {");
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
                            log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                            // Изменяем количество в приходниках на 0.0
                            entity.setQuantity(new BigDecimal(0.0));
                            invoiceStocksList.set(i, entity);
                            this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
                            // Уменьшаем количестов в остатках на количество в приходниках, чтоб не было повторного списания из следующих приходников
                            remainingStockQuantity = remainingStockQuantity - invoiceQuantity;
                            remainingItemStockResponseDto.setQuantityBalance(remainingStockQuantity);
                            this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItemStockResponseDto);
                            log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                        } else {
                            log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                            // Уменьшаем количество в приходниках на количества в остатках
                            entity.setQuantity(new BigDecimal(invoiceQuantity - remainingStockQuantity));
                            invoiceStocksList.set(i, entity);
                            this.invoiceStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, invoiceStocksList);
                            // Изменить количество в остатках на 0.0
                            remainingStockQuantity = 0.0;
                            remainingItemStockResponseDto.setQuantityBalance(remainingStockQuantity);
                            this.remainigStocksMap.get(nomenclatureKey).get(characteristicKey).replace(batchKey, remainingItemStockResponseDto);
                            log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
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
        List<InvoiceEntity> invoiceEntities = invoiceRepository.findAll();

        for (InvoiceEntity invoiceEntity : invoiceEntities) {
            List<InvoiceStocksEntity> entities = invoiceStocksRepository.findAllByRefKey(invoiceEntity.getRefKey());
            for (InvoiceStocksEntity entity : entities) {


                UUID id_nom = entity.getNomenclatureKey();
                UUID id_char = entity.getCharacteristicKey();
                UUID id_b = entity.getBatchKey();

                // Если ключ уже существует, добавляем в существующий список
                if (this.invoiceStocksMap.containsKey(id_nom)) {
                    if(this.invoiceStocksMap.get(id_nom).containsKey(id_char)){
                        if (this.invoiceStocksMap.get(id_nom).get(id_char).containsKey(id_b)) {
                            this.invoiceStocksMap.get(id_nom).get(id_char).get(id_b).add(entity);
                        }
                        else {
                            // Если ключ новый, создаем новый список и добавляем данные
                            List<InvoiceStocksEntity> dataList = new ArrayList<>();
                            dataList.add(entity);

                            this.invoiceStocksMap.get(id_nom).get(id_char).put(id_b, dataList);
                        }
                    } else {
                        List<InvoiceStocksEntity> dataList = new ArrayList<>();
                        dataList.add(entity);
                        Map<UUID, List<InvoiceStocksEntity>> mapLevel1 = new HashMap<>();
                        mapLevel1.put(id_b, dataList);

                        this.invoiceStocksMap.get(id_nom).put(id_char, mapLevel1);
                    }
                } else {
                    List<InvoiceStocksEntity> dataList = new ArrayList<>();
                    dataList.add(entity);
                    Map<UUID, List<InvoiceStocksEntity>> mapLevel1 = new HashMap<>();
                    mapLevel1.put(id_b, dataList);
                    Map<UUID, Map<UUID, List<InvoiceStocksEntity>>> mapLevel2 = new HashMap<>();
                    mapLevel2.put(id_char, mapLevel1);

                    this.invoiceStocksMap.put(id_nom, mapLevel2);
                }
            }
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForInvoiceStocks");
        log.info("{}--{}", this.invoiceStocksMap.size(), this.invoiceStocksMap.hashCode());
    }

    private Map<UUID, List<ExpendStocksEntity>> createMapForExpendStocks(List<ExpendEntity> list) {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForExpendStocks");

        Map<UUID, List<ExpendStocksEntity>> dataMap = new HashMap<>();

        for (ExpendEntity entity : list) {
            UUID id = entity.getRefKey();

            List<ExpendStocksEntity> expendStocksEntities = expendStocksRepository.findAllByRefKey(id);
            if (expendStocksEntities.isEmpty()) {
                log.info("if (expendStocksEntities.isEmpty()) {");
                continue;
            }
            dataMap.put(id, expendStocksEntities);
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForExpendStocks");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private boolean getTrueFalseForBatch(UUID batchKey) {
        return batchKey.compareTo(UUID.fromString("00000000-0000-0000-0000-000000000000")) != 0;
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
}
