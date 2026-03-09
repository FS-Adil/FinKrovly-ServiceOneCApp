package com.example.serviceonec.service.costprice.structure.expend;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendFullEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.repository.expend.ExpendFullRepository;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import com.example.serviceonec.service.expend.ExpendService;
import com.example.serviceonec.service.expend.ExpendStocksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpendFullService {

    private final ExpendService expendService;
    private final ExpendStocksService expendStocksService;
    private final ExpendRepository expendRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final ExpendFullRepository expendFullRepository;

    private List<ExpendEntity> expendEntityList;
    private Map<UUID, List<ExpendStocksEntity>> dataMap;


    public void getAllExpend(
            UUID organizationId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        expendService.getAllExpend(
                organizationId,
                dateFrom,
                dateTo
        );
    }

    public void getAllExpendStocks() {
        log.debug("🔍 Поиск всех расходных накладных в БД отсортированных по дате");
        this.expendEntityList = expendRepository.findAllByOrderByDateDesc();

        log.debug("🔍 Загрузка запасов расходных накладных");

        assert this.expendEntityList != null;
        List<UUID> refKeys = this.expendEntityList.stream()
                .map(ExpendEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        log.debug("Получено {} уникальных refKey расходников", refKeys.size());

        List<ExpendStocksEntity> allExpendStocks = expendStocksRepository.findAllByRefKeyIn(refKeys);
        log.debug("Загружено {} записей запасов из БД", allExpendStocks.size());

        this.dataMap = allExpendStocks.stream()
                .collect(Collectors.groupingBy(ExpendStocksEntity::getRefKey));

        List<UUID> missingRefKeys = refKeys.stream()
                .filter(key -> !this.dataMap.containsKey(key))
                .toList();

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} расходников без запасов, загружаем из 1С...", missingRefKeys.size());
            try {
                Map<UUID, List<ExpendStocksEntity>> foundMissing = expendStocksService
                        .findExpendStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    foundMissing.forEach((key, value) ->
                            this.dataMap.merge(key, value, (v1, v2) -> {
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
        log.debug("✅ Мапа расходников создана: {} документов с запасами", this.dataMap.size());
    }

    public void fillExpendFullEntity() {
        List<ExpendFullEntity> expendFullEntities = new ArrayList<>();

        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);

        for (ExpendEntity expend : this.expendEntityList) {
            int expendNum = processedExpend.incrementAndGet();
            UUID expendRefKey = expend.getRefKey();

            if (expendNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} расходников", expendNum, this.expendEntityList.size());
            }

            if (!this.dataMap.containsKey(expendRefKey)) {
                log.debug("⚠️ Расходник {}: запасы не найдены", expendRefKey);
                continue;
            }

            for (ExpendStocksEntity expendStocksEntity : this.dataMap.get(expendRefKey)) {
                processedStocks.incrementAndGet();

                LocalDateTime date = expend.getDate();
                String documentType = "ПРОДАЖА";
                String documentTypeOneC = "Расходная накладная";
                UUID organizationKey = expend.getOrganizationKey();
                UUID structuralUnitKey = expend.getStructuralUnitKey();
                UUID customerOrder = expend.getDocOrder();

                UUID nomenclatureKey = expendStocksEntity.getNomenclatureKey();
                UUID characteristicKey = expendStocksEntity.getCharacteristicKey();
                UUID batchKey = expendStocksEntity.getBatchKey();

                String number = expend.getNumber();
                BigDecimal price = expendStocksEntity.getPrice().setScale(3, RoundingMode.HALF_UP);;
                BigDecimal quantity = expendStocksEntity.getQuantity().setScale(3, RoundingMode.HALF_UP);


                expendFullEntities.add(ExpendFullEntity.builder()
                                .date(date)
                                .documentType(documentType)
                                .documentTypeOneC(documentTypeOneC)
                                .number(number)
                                .refKey(expendRefKey)
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
        }
        expendFullRepository.saveAll(expendFullEntities);

        log.info("📦 Всего расходников: {}", this.expendEntityList.size());
        log.info("📦 Всего позиций запасов: {}", processedStocks.get());
    }
}
