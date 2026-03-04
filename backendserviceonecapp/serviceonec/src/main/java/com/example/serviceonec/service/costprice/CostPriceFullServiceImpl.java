package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
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

        // Шаг 2:

        // Шаг 3:

        // Шаг 4:

        // Шаг 5:

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
