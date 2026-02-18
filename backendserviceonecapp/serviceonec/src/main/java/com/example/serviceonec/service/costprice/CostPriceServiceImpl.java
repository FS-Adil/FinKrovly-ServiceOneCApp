package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.assembly.output.AssemblyExpendControllerOutput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
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

    private Map<UUID, List<InvoiceStocksEntity>> invoiceStocksMap;

    private final ExpendRepository expendRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final NomenclatureRepository nomenclatureRepository;

    @Override
    public List<CostPriceControllerOutput> getAllCostPrice() {
        log.info("--------> CostPriceServiceImpl --------> getAllCostPrice");
        List<CostPriceControllerOutput> list = new ArrayList<>();

        List<ExpendEntity> expendList = findAllExpend();
        Map<UUID, List<ExpendStocksEntity>> expendStocksMap = createMapForExpendStocks();
        this.invoiceStocksMap = createMapForInvoiceStocks();
        Map<UUID, String> nomenclatureMap = createMapForNomenclature();


        for (ExpendEntity expend : expendList) {
//            log.info("for ---> ExpendEntity expend : expendList");
            UUID expendRefKey = expend.getRefKey();
            if (!expendStocksMap.containsKey(expendRefKey)) {
                log.info("В Запасы расходниках такой позиции нет - {}", expendRefKey);
                continue;
            }

            for (ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)) {
//                log.info("for -----> ExpendStocksEntity expendStocksEntity : expendStocksMap.get(expendRefKey)");
                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                if (!invoiceStocksMap.containsKey(nomenclatureKey)) {
//                    log.info("В приходниках такой номенклатуры нет - {}", nomenclatureKey);
                    continue;
                }

                String name = nomenclatureMap.get(nomenclatureKey);
                double price = expendStocksEntity.getPrice().doubleValue();
                double quantity = expendStocksEntity.getQuantity().doubleValue();

                double cost = getCostForNomenclature(nomenclatureKey, quantity);
                if (cost == 0.0) {
//                    log.info("В приходнике количество меньше чем в расходнике - {}", nomenclatureKey);
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
        return list;
    }

    private double getCostForNomenclature(UUID nomenclatureKey, double quantity){
        List<InvoiceStocksEntity> invoiceStocksList = this.invoiceStocksMap.get(nomenclatureKey);

        for (int i = 0; i < invoiceStocksList.size(); i++) {
            InvoiceStocksEntity entity = invoiceStocksList.get(i);

            double invoiceQuantity = entity.getQuantity().doubleValue();

            if ( invoiceQuantity >= quantity) {
//                log.info("до ------>{}------>{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                entity.setQuantity(new BigDecimal(invoiceQuantity - quantity));
                invoiceStocksList.set(i, entity);
//                log.info("после ------>{}------->{}", invoiceStocksList.get(i).getQuantity(), nomenclatureKey);
                this.invoiceStocksMap.replace(nomenclatureKey, invoiceStocksList);

                return entity.getPrice().doubleValue();
            }
        }
//        log.info("Вход в метод getCostForNomenclature с данным количеством ------>{}------>{}", quantity, nomenclatureKey);
        return 0.0;
    }

    private List<ExpendEntity> findAllExpend() {
        return expendRepository.findAll();
    }

    private List<InvoiceEntity> findAllInvoice() {
        return invoiceRepository.findAll();
    }

    private Map<UUID, String> createMapForNomenclature() {
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

    private Map<UUID, List<InvoiceStocksEntity>> createMapForInvoiceStocks() {
        List<InvoiceStocksEntity> entities = invoiceStocksRepository.findAll();
        List<InvoiceEntity> invoiceEntities = invoiceRepository.findAll();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, List<InvoiceStocksEntity>> dataMap = new HashMap<>(initialCapacity);

        for (InvoiceEntity invoiceEntity : invoiceEntities) {
            for (InvoiceStocksEntity entity : entities) {
                if (entity.getRefKey().compareTo(invoiceEntity.getRefKey()) == 0) {
                    UUID id = entity.getNomenclatureKey();

                    // Если ключ уже существует, добавляем в существующий список
                    if (dataMap.containsKey(id)) {
                        dataMap.get(id).add(entity);
                    } else {
                        // Если ключ новый, создаем новый список и добавляем данные
                        List<InvoiceStocksEntity> dataList = new ArrayList<>();
                        dataList.add(entity);
                        dataMap.put(id, dataList);
                    }
                }
            }
        }

//        for (InvoiceStocksEntity entity : entities) {
//            UUID id = entity.getNomenclatureKey();
//
//            // Если ключ уже существует, добавляем в существующий список
//            if (dataMap.containsKey(id)) {
//                dataMap.get(id).add(entity);
//            } else {
//                // Если ключ новый, создаем новый список и добавляем данные
//                List<InvoiceStocksEntity> dataList = new ArrayList<>();
//                dataList.add(entity);
//                dataMap.put(id, dataList);
//            }
//        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForInvoiceStocks");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }

    private Map<UUID, List<ExpendStocksEntity>> createMapForExpendStocks() {
        List<ExpendStocksEntity> entities = expendStocksRepository.findAll();

        // Оптимизация: задаем правильную начальную емкость
        // load factor = 0.75, поэтому размер = (количество / 0.75) + 1
        int initialCapacity = (int) (entities.size() / 0.75) + 1;
        Map<UUID, List<ExpendStocksEntity>> dataMap = new HashMap<>(initialCapacity);

        for (ExpendStocksEntity entity : entities) {
            UUID id = entity.getRefKey();

            // Если ключ уже существует, добавляем в существующий список
            if (dataMap.containsKey(id)) {
                dataMap.get(id).add(entity);
            } else {
                // Если ключ новый, создаем новый список и добавляем данные
                List<ExpendStocksEntity> dataList = new ArrayList<>();
                dataList.add(entity);
                dataMap.put(id, dataList);
            }
        }

        log.info("Finish -----> CostPriceServiceImpl ------> createMapForExpendStocks");
        log.info("{}--{}", dataMap.size(), dataMap.hashCode());
        return dataMap;
    }
}
