package com.example.serviceonec.service.inventorycost;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.controller.inventorycost.output.InventoryCostControllerOutput;
import com.example.serviceonec.service.costprice.structure.invoice.InvoiceFullService;
import com.example.serviceonec.service.costprice.structure.resultingincome.ResultingIncomeFullService;
import com.example.serviceonec.service.inventorycost.structure.InventoryCostCalculationService;
import com.example.serviceonec.service.remaining.RemainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryCostServiceImpl implements InventoryCostService{

    private final RemainingService remainingService;
    private final InvoiceFullService invoiceFullService;
    private final InventoryCostCalculationService inventoryCostCalculationService;
    private final ResultingIncomeFullService resultingIncomeFullService;

    @Override
    public List<InventoryCostControllerOutput> getAllInventoryCost(UUID organizationId, LocalDateTime endDate) {

        long methodStartTime = System.currentTimeMillis();
        log.info("🚀 ===== НАЧАЛО РАСЧЕТА СЕБЕСТОИМОСТИ ОСТАТКОВ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Остатки на дату: {}", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        long stepStart;

        List<InventoryCostControllerOutput> list = new ArrayList<>();

        // Шаг 1:
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг-1.1/5: Получаем остатки товаров из 1с по id организации и сохраняем в БД");
        remainingService.getAllRemaining(organizationId, endDate);
        log.info("✅ Шаг-1/5 заверщен за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 2:
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг-2.1/5: Получаем список Приходных накладных (Поступление от поставщика)(Приходников) из 1с и сохраняем в БД");
        invoiceFullService.getAllInvoice(organizationId, endDate);
        log.info("📥 Шаг-2.2/5: Получаем список ЗАПАСОВ приходных накладных на основе Приходников из бд или из 1с если в бд их нет");
        invoiceFullService.getAllInvoiceStocks(endDate);
        log.info("📥 Шаг-2.3/5: Формируем InvoiceFullEntity и сохраняем в БД");
        invoiceFullService.fillInvoiceFullEntity();
        log.info("✅ Шаг-2/5 заверщен за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 3:
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг-3.1/5: Cобираем таблицу ПРИХОДОВ в одну (Приходная накладная от поставщика + Производство) и сортируем по дате");
        resultingIncomeFullService.addResultingIncomeFullTable();
        log.info("✅ Шаг-3/5 заверщен за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 4:
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг-4.1/5: Загрузка Остатков из БД...");
        inventoryCostCalculationService.findAllRemainingStocks();
        log.info("📥 Шаг-4.2/5: Загрузка Результирующий ПРИХОД из БД...");
        inventoryCostCalculationService.findAllResultingIncomeFullEntity();
        log.info("📥 Шаг-4.3/5: Расчет Себестоимости остатков");
        inventoryCostCalculationService.findInventoryCostPrice(organizationId);
        log.info("📥 Шаг-4.4/5: Формируем итоговый List");
        list = inventoryCostCalculationService.getList();
        log.info("✅ Шаг-4/5 заверщен за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 7:
        // Агрегация результатов
        log.info("🔄 5.1/5:Агрегация результатов...");
        stepStart = System.currentTimeMillis();
        List<InventoryCostControllerOutput> aggregated = aggregateOnlyFast(list);
        log.info("✅ Шаг-5/5 заверщен за {} мс ", System.currentTimeMillis() - stepStart);

        long totalTime = System.currentTimeMillis() - methodStartTime;
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("🏁 ===== ЗАВЕРШЕНИЕ РАСЧЕТА СЕБЕСТОИМОСТИ ОСТАТКОВ=====");

        return aggregated;
    }

    private List<InventoryCostControllerOutput> aggregateOnlyFast(List<InventoryCostControllerOutput> products) {
        log.debug("🔄 Агрегация результатов");

        if (products == null || products.isEmpty()) {
            log.debug("Список продуктов пуст");
            return new ArrayList<>();
        }

        log.info("Начальное количество записей: {}", products.size());

        Map<String, InventoryCostControllerOutput> map = new HashMap<>(products.size());

        for (InventoryCostControllerOutput p : products) {
            String key = p.getName() + "|" + p.getCharacteristic() + "|" + p.getBatch() + "|" + p.getCost();

            InventoryCostControllerOutput existing = map.get(key);
            if (existing == null) {
                String productName = p.getName() != null ? p.getName() : "Без имени";
                String productCharacteristic = p.getCharacteristic() != null ? p.getCharacteristic() : "Без характеристики";
                String productBatch = p.getBatch() != null ? p.getBatch() : "Без партии";

                map.put(key, InventoryCostControllerOutput.builder()
                        .refKey(p.getRefKey())
                        .name(productName)
                        .characteristic(productCharacteristic)
                        .batch(productBatch)
                        .measurementUnit(p.getMeasurementUnit())
                        .quantity(p.getQuantity())
                        .cost(p.getCost().multiply(p.getQuantity()))
                        .build()
                );
            } else {
                existing.setQuantity(existing.getQuantity().add(p.getQuantity()));
                existing.setCost(existing.getCost().add(p.getCost().multiply(p.getQuantity())));
            }
        }

        List<InventoryCostControllerOutput> result = new ArrayList<>(map.values());
        result.sort(Comparator.comparing(InventoryCostControllerOutput::getName));

        log.debug("✅ Агрегация завершена: {} -> {} записей", products.size(), result.size());
        return result;
    }
}
