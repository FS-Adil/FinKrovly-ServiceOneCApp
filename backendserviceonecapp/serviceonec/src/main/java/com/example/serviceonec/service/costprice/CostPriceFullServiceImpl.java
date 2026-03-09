package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import com.example.serviceonec.service.costprice.structure.expend.ExpendFullService;
import com.example.serviceonec.service.costprice.structure.invoice.InvoiceFullService;
import com.example.serviceonec.service.costprice.structure.production.ProductionFullService;
import com.example.serviceonec.service.remaining.RemainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostPriceFullServiceImpl implements CostPriceFullService {

    private final ExpendFullService expendFullService;
    private final InvoiceFullService invoiceFullService;
    private final ProductionFullService productionFullService;
    private final RemainingService remainingService;

    @Override
    public List<CostPriceControllerOutput> getAllCostPrice(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        long methodStartTime = System.currentTimeMillis();
        log.info("🚀 ===== НАЧАЛО РАСЧЕТА СЕБЕСТОИМОСТИ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Дата начала периода: {}", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("Дата окончания периода: {}", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<CostPriceControllerOutput> list = new ArrayList<>();

        long stepStart;

        // Шаг 1:
        // Получаем список Расходных накладных(Расходников) из 1с ПРОДАЖА
        // Получаем список ЗАПАСОВ расходных накладных на основе Расходников из бд или из 1с если в бд их нет
        // Формируем ExpendFullEntity
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг 1.1/7: Получаем список Расходных накладных(Расходников) из 1с ПРОДАЖА");
        expendFullService.getAllExpend(organizationId, startDate, endDate);
        log.info("📥 Шаг 1.2/7: Получаем список ЗАПАСОВ расходных накладных на основе Расходников из бд или из 1с если в бд их нет");
        expendFullService.getAllExpendStocks();
        log.info("📥 Шаг 1.3/7: Формируем ExpendFullEntity");
        expendFullService.fillExpendFullEntity();
        log.info("✅ Сформировано ExpendFullEntity за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 2:
        // Получаем список Приходных накладных (Поступление от поставщика)(Приходников) из 1с ПРИХОД
        // Получаем список ЗАПАСОВ приходных накладных на основе Приходников из бд или из 1с если в бд их нет
        // Формируем InvoiceFullEntity
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг 2.1/7: Получаем список Приходных накладных (Поступление от поставщика)(Приходников) из 1с ПРИХОД");
        invoiceFullService.getAllInvoice(organizationId, endDate);
        log.info("📥 Шаг 2.2/7: Получаем список ЗАПАСОВ приходных накладных на основе Приходников из бд или из 1с если в бд их нет");
        invoiceFullService.getAllInvoiceStocks();
        log.info("📥 Шаг 2.3/7: Формируем InvoiceFullEntity");
        invoiceFullService.fillInvoiceFullEntity();
        log.info("✅ Сформировано InvoiceFullEntity за {} мс ", System.currentTimeMillis() - stepStart);

        // Шаг 3:
        // Получаем остатки товаров из 1c РАСХОД
        log.info("📥 Шаг 3.1/7: Получаем остатки товаров из 1с РАСХОД");
        remainingService.getAllRemaining(organizationId, endDate);

        // Шаг 4:
        // Получаем список документов по производству, запасов на производство, продукция, распределение запасов ПРИХОД
        // Получаем список всех документов по производству исходя из данных из расходников
        // Расчет себестоимости по производству исходя из приходных накладных и остатков в организации
        // Формируем ProductionFullEntity
        stepStart = System.currentTimeMillis();
        log.info("📥 Шаг 4.1/7: Получаем список документов по производству, запасов на производство, продукция, распределение запасов ПРИХОД");
        productionFullService.getAllProduction(organizationId, startDate, endDate);
        log.info("📥 Шаг 4.2/7: Получаем список всех документов по производству исходя из данных из расходников");
        productionFullService.addAllProduction();
        log.info("📥 Шаг 4.3/7: Расчет себестоимости по производству исходя из приходных накладных и остатков в организации");
        productionFullService.calculationCostPriceProduction();
        log.info("📥 Шаг 4.4/7: Формируем ProductionFullEntity");
        productionFullService.fillProductionFullEntity();
        log.info("✅ Сформировано ProductionFullEntity за {} мс ", System.currentTimeMillis() - stepStart);


        // Шаг 5:
        // Выравниваем ПРИХОД относительно РАСХОДОВ

        // Шаг 6:
        // Соотносим ПРОДАЖИ к ПРИХОДУ для получения себестоимости, фильтруем по Номенклатуре->Характеристике->Партии
        // Формируем итоговый List

        // Шаг 7:
        // Агрегация результатов
        log.info("🔄 Агрегация результатов...");
        stepStart = System.currentTimeMillis();
        List<CostPriceControllerOutput> aggregated = aggregateOnlyFast(list);
        log.info("✅ Агрегация завершена за {} мс, получено {} уникальных записей",
                System.currentTimeMillis() - stepStart, aggregated.size());

        long totalTime = System.currentTimeMillis() - methodStartTime;
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("🏁 ===== ЗАВЕРШЕНИЕ РАСЧЕТА СЕБЕСТОИМОСТИ =====");
        return aggregated;
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
