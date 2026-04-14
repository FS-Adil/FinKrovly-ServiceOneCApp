package com.example.serviceonec.service.costprice.structure.invoice;

import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceFullEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.repository.invoice.InvoiceFullRepository;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import com.example.serviceonec.service.invoice.InvoiceService;
import com.example.serviceonec.service.invoice.InvoiceStocksService;
import jakarta.transaction.Transactional;
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
public class InvoiceFullService {

    private final InvoiceService invoiceService;
    private final InvoiceStocksService invoiceStocksService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final InvoiceFullRepository invoiceFullRepository;

    private List<InvoiceEntity> invoiceEntityList = new ArrayList<>();
    private Map<UUID, List<InvoiceStocksEntity>> dataMap = new HashMap<>();

    public void getAllInvoice(
            UUID organizationId,
            LocalDateTime dateTo
    ) {
        invoiceService.getAllInvoice(
                organizationId,
                dateTo
        );
    }

    public void getAllInvoiceStocksNew () {
        log.debug("🔍 Поиск всех приходных накладных в БД отсортированных по дате и по типу");
        this.invoiceEntityList.clear();
        this.dataMap.clear();

        String operationType = "ПоступлениеОтПоставщика";
        this.invoiceEntityList = invoiceRepository.findALlByOperationTypeOrdered(operationType);

        log.debug("🔍 Загрузка запасов приходных накладных");
        List<UUID> refKeys = this.invoiceEntityList.stream()
                .map(InvoiceEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (refKeys.isEmpty()) {
            log.warn("⚠️ Не найдено приходных накладных с типом операции: {}", operationType);
        }

        log.debug("Найдено {} приходных накладных с типом операции '{}'", refKeys.size(), operationType);

        List<InvoiceStocksEntity> allStocks = new ArrayList<>();

        Set<UUID> missingRefKeys = new HashSet<>(refKeys);

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} приходников без запасов, загружаем из 1С...", missingRefKeys.size());

            invoiceStocksRepository.deleteAll();

            try {
                invoiceStocksService.findInvoiceStocksByIds(missingRefKeys);

                List<InvoiceStocksEntity> foundMissing = invoiceStocksRepository.findAll();

                if (!foundMissing.isEmpty()) {
                    allStocks.addAll(foundMissing);
                    log.info("✅ Загружено {} записей из 1С", foundMissing.size());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < refKeys.size(); i++) {
            orderMap.put(refKeys.get(i), i);
        }

        allStocks.sort(Comparator.comparing(s -> orderMap.get(s.getRefKey())));

        this.dataMap = allStocks.stream()
                .collect(Collectors.groupingBy(InvoiceStocksEntity::getRefKey));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.dataMap.size());
    }

    @Transactional
    public void getAllInvoiceStocks (LocalDateTime dateTo) {
        log.debug("🔍 Поиск всех приходных накладных в БД отсортированных по дате и по типу");
        this.invoiceEntityList.clear();
        this.dataMap.clear();

        String operationType = "ПоступлениеОтПоставщика";

        List<InvoiceEntity> invoiceEntitiesDateRange = invoiceRepository.findAllByOperationTypeAndDateRange(
                operationType,
                dateTo.minusMonths(3),
                dateTo
                );

        List<UUID> refKeysDateRange = invoiceEntitiesDateRange.stream()
                .map(InvoiceEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Удаление по списку ключей
        invoiceStocksRepository.deleteAllByRefKeyIn(refKeysDateRange);

        if (!refKeysDateRange.isEmpty()) {
            log.info("🔄 Обнаружено {} приходников  за ВЫБРАННЫЙ {} - {} период, загружаем из 1С...", refKeysDateRange.size(), dateTo.minusMonths(12), dateTo);
            try {
                invoiceStocksService.findInvoiceStocksByIds(new HashSet<>(refKeysDateRange));
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        this.invoiceEntityList = invoiceRepository.findALlByOperationTypeOrdered(operationType);

        log.debug("🔍 Загрузка запасов приходных накладных");
        List<UUID> refKeys = this.invoiceEntityList.stream()
                .map(InvoiceEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (refKeys.isEmpty()) {
            log.warn("⚠️ Не найдено приходных накладных с типом операции: {}", operationType);
        }

        log.debug("Найдено {} приходных накладных с типом операции '{}'", refKeys.size(), operationType);

        List<InvoiceStocksEntity> allStocks = invoiceStocksRepository.findAllByRefKeyIn(refKeys);

        if (allStocks.isEmpty()) {
            log.warn("⚠️ Не найдено запасов для приходных накладных");
            return;
        }

        log.debug("Загружено {} записей запасов из БД", allStocks.size());

        Set<UUID> foundRefKeys = allStocks.stream()
                .map(InvoiceStocksEntity::getRefKey)
                .collect(Collectors.toSet());

        Set<UUID> missingRefKeys = new HashSet<>(refKeys);
        missingRefKeys.removeAll(foundRefKeys);

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} приходников без запасов, загружаем из 1С...", missingRefKeys.size());
            try {
                List<InvoiceStocksEntity> foundMissing = invoiceStocksService
                        .findInvoiceStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    allStocks.addAll(foundMissing);
                    log.info("✅ Загружено {} записей из 1С", foundMissing.size());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < refKeys.size(); i++) {
            orderMap.put(refKeys.get(i), i);
        }

        allStocks.sort(Comparator.comparing(s -> orderMap.get(s.getRefKey())));

        this.dataMap = allStocks.stream()
                .collect(Collectors.groupingBy(InvoiceStocksEntity::getRefKey));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.dataMap.size());
    }

    public void getAllInvoiceStocksOld () {
        log.debug("🔍 Поиск всех приходных накладных в БД отсортированных по дате и по типу");
        this.invoiceEntityList.clear();
        this.dataMap.clear();

        String operationType = "ПоступлениеОтПоставщика";
        this.invoiceEntityList = invoiceRepository.findALlByOperationTypeOrdered(operationType);

        log.debug("🔍 Загрузка запасов приходных накладных");
        List<UUID> refKeys = this.invoiceEntityList.stream()
                .map(InvoiceEntity::getRefKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (refKeys.isEmpty()) {
            log.warn("⚠️ Не найдено приходных накладных с типом операции: {}", operationType);
        }

        log.debug("Найдено {} приходных накладных с типом операции '{}'", refKeys.size(), operationType);

        List<InvoiceStocksEntity> allStocks = invoiceStocksRepository.findAllByRefKeyIn(refKeys);

        if (allStocks.isEmpty()) {
            log.warn("⚠️ Не найдено запасов для приходных накладных");
            return;
        }

        log.debug("Загружено {} записей запасов из БД", allStocks.size());

        Set<UUID> foundRefKeys = allStocks.stream()
                .map(InvoiceStocksEntity::getRefKey)
                .collect(Collectors.toSet());

        Set<UUID> missingRefKeys = new HashSet<>(refKeys);
        missingRefKeys.removeAll(foundRefKeys);

        if (!missingRefKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} приходников без запасов, загружаем из 1С...", missingRefKeys.size());
            try {
                List<InvoiceStocksEntity> foundMissing = invoiceStocksService
                        .findInvoiceStocksByIds(missingRefKeys);

                if (foundMissing != null && !foundMissing.isEmpty()) {
                    allStocks.addAll(foundMissing);
                    log.info("✅ Загружено {} записей из 1С", foundMissing.size());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка загрузки недостающих запасов: {}", e.getMessage(), e);
            }
        }

        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < refKeys.size(); i++) {
            orderMap.put(refKeys.get(i), i);
        }

        allStocks.sort(Comparator.comparing(s -> orderMap.get(s.getRefKey())));

        this.dataMap = allStocks.stream()
                .collect(Collectors.groupingBy(InvoiceStocksEntity::getRefKey));

        log.debug("✅ Мапа приходников создана: {} номенклатур", this.dataMap.size());
    }

    public void fillInvoiceFullEntity () {
        List<InvoiceFullEntity> invoiceFullEntities = new ArrayList<>();

        invoiceFullRepository.deleteAll();
        log.info("Очишаем таблицу invoice_full");

        AtomicInteger processedExpend = new AtomicInteger(0);
        AtomicInteger processedStocks = new AtomicInteger(0);

        for (InvoiceEntity invoice : this.invoiceEntityList) {
            int invoiceNum = processedExpend.incrementAndGet();
            UUID invoiceRefKey = invoice.getRefKey();

            if (invoiceNum % 100 == 0) {
                log.debug("⏳ Обработано {} из {} расходников", invoiceNum, this.invoiceEntityList.size());
            }

            if (!this.dataMap.containsKey(invoiceRefKey)) {
                log.debug("⚠️ Приходник {}: запасы не найдены", invoiceRefKey);
                continue;
            }

            for (InvoiceStocksEntity invoiceStocksEntity : this.dataMap.get(invoiceRefKey)) {
                processedStocks.incrementAndGet();

                LocalDateTime date = invoice.getDate();
                String documentType = "ПРИХОД";
                String documentTypeOneC = "Приходная накладная от поставщиков";
                UUID organizationKey = invoice.getOrganizationKey();
//                UUID structuralUnitKey = invoice.getStructuralUnitKey();
//                UUID customerOrder = invoice.getDocOrder();

                UUID nomenclatureKey = invoiceStocksEntity.getNomenclatureKey();
                UUID characteristicKey = invoiceStocksEntity.getCharacteristicKey();
                UUID batchKey = invoiceStocksEntity.getBatchKey();

                String number = invoice.getNumber();
                BigDecimal price = invoiceStocksEntity.getPrice().setScale(3, RoundingMode.HALF_UP);;
                BigDecimal quantity = invoiceStocksEntity.getQuantity().setScale(3, RoundingMode.HALF_UP);


                invoiceFullEntities.add(InvoiceFullEntity.builder()
                        .date(date)
                        .documentType(documentType)
                        .documentTypeOneC(documentTypeOneC)
                        .number(number)
                        .refKey(invoiceRefKey)
                        .organizationKey(organizationKey)
                        .structuralUnitKey(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .customerOrder(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .nomenclatureKey(nomenclatureKey)
                        .characteristicKey(characteristicKey)
                        .batchKey(batchKey)
                        .quantity(quantity)
                        .price(price)
                        .build());
            }
        }
        invoiceFullRepository.saveAll(invoiceFullEntities);

        log.info("📦 Всего приходников: {}", this.invoiceEntityList.size());
        log.info("📦 Всего позиций запасов: {}", processedStocks.get());
    }
}
