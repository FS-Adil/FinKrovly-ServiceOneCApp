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
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostPriceServiceImpl implements CostPriceService {

    private final Map<UUID, List<InvoiceStocksEntity>> invoiceStocksMap = new HashMap<>();
    private final Map<UUID, RemainingItemStockResponseDto> remainigStocksMap = new HashMap<>();

    private final ExpendRepository expendRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final NomenclatureRepository nomenclatureRepository;

    private final OneCProperties oneCProperties;
    private final RestClientConfig restClientConfig;

    @Override
    public List<CostPriceControllerOutput> getAllCostPrice(UUID organizationId) {
        log.info("Start--------> CostPriceServiceImpl --------> getAllCostPrice");
        List<CostPriceControllerOutput> list = new ArrayList<>();

        List<ExpendEntity> expendList = findAllExpend();
        Map<UUID, List<ExpendStocksEntity>> expendStocksMap = createMapForExpendStocks(expendList);
        createMapForInvoiceStocks(); // this.invoiceStocksMap создаем на основе данных из бд
        createMapForRemainigStocks(organizationId); // this.remainigStocksMap создаем структуру для хранения остатков товаров по id организации
        updateMapForInvoiceStocks(); // this.invoiceStocksMap изменяем исходя из остатков на складе, то есть убираем из структуры количество, которое числится в остатке

        Map<UUID, String> nomenclatureMap = createMapForNomenclature();


        for (ExpendEntity expend : expendList) {
            log.info("for ---> ExpendEntity expend : expendList");
            UUID expendRefKey = expend.getRefKey();
            if (!expendStocksMap.containsKey(expendRefKey)) {
                log.info("В Запасы расходниках такой позиции нет - {}", expendRefKey);
                continue;
            }

            log.info("expendList ---- {} ------->{}",
                    expendRefKey,
                    expendStocksMap.size());

            for (ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)) {
                log.info("for -----> ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)");
                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                if (!invoiceStocksMap.containsKey(nomenclatureKey)) {
                    log.info("В приходниках такой номенклатуры нет - {}", nomenclatureKey);
                    continue;
                }

                String name = nomenclatureMap.get(nomenclatureKey);
                double price = expendStocksEntity.getPrice().doubleValue();
                double quantity = expendStocksEntity.getQuantity().doubleValue();

                double cost = getCostForNomenclature(nomenclatureKey, quantity);
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

    private double getCostForNomenclature(UUID nomenclatureKey, double quantity){
        log.info("Start--------> CostPriceServiceImpl --------> getCostForNomenclature");
        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap.get(nomenclatureKey);

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            double invoiceQuantity = entity.getQuantity().doubleValue();

            if ( invoiceQuantity >= quantity) {
                log.info("до ------>{}------>{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                entity.setQuantity(new BigDecimal(invoiceQuantity - quantity));
                invoiceStocksList.set(i, entity);
                log.info("после ------>{}------->{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                this.invoiceStocksMap.replace(nomenclatureKey, invoiceStocksList);

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
        List<UUID> keysList = new ArrayList<>(this.invoiceStocksMap.keySet());

        for (UUID nomenclatureKey : keysList) {

            List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap.get(nomenclatureKey);

            RemainingItemStockResponseDto remainingItemStockResponseDto = this.remainigStocksMap.get(nomenclatureKey);
            if (remainingItemStockResponseDto == null) {
                continue;
            }
            double remainingStockQuantity = remainingItemStockResponseDto.getQuantityBalance();

            for (int i = 0; i < invoiceStocksList.size(); i++) {
                InvoiceStocksEntity entity = invoiceStocksList.get(i);

                double invoiceQuantity = entity.getQuantity().doubleValue();
                if (invoiceQuantity <= remainingStockQuantity) {
                    log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
//                    invoiceStocksList.remove(i);
                    entity.setQuantity(new BigDecimal(0.0));
                    invoiceStocksList.set(i, entity);
                    this.invoiceStocksMap.replace(nomenclatureKey, invoiceStocksList);

                    remainingStockQuantity = remainingStockQuantity - invoiceQuantity;
                    log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                } else {
                    log.info("{} до ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                    entity.setQuantity(new BigDecimal(invoiceQuantity - remainingStockQuantity));
                    invoiceStocksList.set(i, entity);
                    this.invoiceStocksMap.replace(nomenclatureKey, invoiceStocksList);
                    remainingStockQuantity = 0.0;
                    log.info("{} после ---> {}-{}-{}", i, entity.getQuantity().doubleValue(), remainingStockQuantity, nomenclatureKey);
                    break;
                }
            }
        }
        log.info("Finish --------> CostPriceServiceImpl --------> updateMapForInvoiceStocks");
    }

    private void createMapForRemainigStocks(UUID organizationId) {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForRemainigStocks");
        RemainingStockResponseDto remainingStockResponseDto = getAllStocks(organizationId);
        List<RemainingItemStockResponseDto> entities = remainingStockResponseDto.getValue();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;

        for (RemainingItemStockResponseDto entity : entities) {
            this.remainigStocksMap.put(entity.getNomenclatureKey(), entity);
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForRemainigStocks");
        log.info("{}--{}", this.remainigStocksMap.size(), this.remainigStocksMap.hashCode());
    }

    private void createMapForInvoiceStocks() {
        log.info("Start --------> CostPriceServiceImpl --------> createMapForInvoiceStocks");
        List<InvoiceStocksEntity> entities = invoiceStocksRepository.findAll();
        List<InvoiceEntity> invoiceEntities = invoiceRepository.findAll();

        for (InvoiceEntity invoiceEntity : invoiceEntities) {
            for (InvoiceStocksEntity entity : entities) {
                if (entity.getRefKey().compareTo(invoiceEntity.getRefKey()) == 0) {
                    UUID id = entity.getNomenclatureKey();

                    // Если ключ уже существует, добавляем в существующий список
                    if (this.invoiceStocksMap.containsKey(id)) {
                        this.invoiceStocksMap.get(id).add(entity);
                    } else {
                        // Если ключ новый, создаем новый список и добавляем данные
                        List<InvoiceStocksEntity> dataList = new ArrayList<>();
                        dataList.add(entity);
                        this.invoiceStocksMap.put(id, dataList);
                    }
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
            if (expendStocksEntities == null) {
                continue;
            }
            dataMap.put(id, expendStocksEntities);
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForExpendStocks");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private RemainingStockResponseDto getAllStocks(UUID guid) {
        log.info("------> CostPriceServiceImpl -------> getAllStocks");

        String url = String.format("/AccumulationRegister_Запасы/Balance(" +
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
