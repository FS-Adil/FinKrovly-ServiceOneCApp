package com.example.serviceonec.service.costprice.structure.resultingincome;


import com.example.serviceonec.model.entity.invoice.InvoiceFullEntity;
import com.example.serviceonec.model.entity.production.ProductionFullEntity;
import com.example.serviceonec.model.entity.resultingincome.ResultingIncomeFullEntity;
import com.example.serviceonec.repository.invoice.InvoiceFullRepository;
import com.example.serviceonec.repository.production.ProductionFullRepository;
import com.example.serviceonec.repository.resultingincome.ResultingIncomeFullRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResultingIncomeFullService {

    private final InvoiceFullRepository invoiceFullRepository;
    private final ProductionFullRepository productionFullRepository;
    private final ResultingIncomeFullRepository resultingIncomeFullRepository;


    // Пороговые значения для выбора метода (можно настроить под ваши требования)
    private static final int SMALL_LIST_THRESHOLD = 10_000;      // до 10k - stream API
    private static final int MEDIUM_LIST_THRESHOLD = 100_000;

    public void deleteProductionFullRepository() {
        productionFullRepository.deleteAll();
    }

    public void addResultingIncomeFullTable() {

        resultingIncomeFullRepository.deleteAll();
        log.info("✓ Таблица resulting_income_full очищена");

        List<InvoiceFullEntity> invoiceFullEntityList = invoiceFullRepository.findAll();
        List<ProductionFullEntity> productionFullEntityList = productionFullRepository.findAll();

        resultingIncomeFullRepository.saveAll(
                mergeListsAuto(invoiceFullEntityList, productionFullEntityList)
        );

    }

    /**
     * Универсальный метод, который автоматически выбирает оптимальный способ объединения
     * на основе количества элементов во входных списках
     */
    private List<ResultingIncomeFullEntity> mergeListsAuto(
            List<InvoiceFullEntity> invoiceList,
            List<ProductionFullEntity> productionList) {

        long startTime = System.currentTimeMillis();

        int invoiceSize = invoiceList.size();
        int productionSize = productionList.size();
        int totalSize = invoiceSize + productionSize;

        log.info("Автовыбор метода объединения. Всего элементов: {} (Invoice: {}, Production: {})",
                totalSize, invoiceSize, productionSize);

        String selectedMethod;
        List<ResultingIncomeFullEntity> result;

        if (totalSize <= SMALL_LIST_THRESHOLD) {
            selectedMethod = "Stream API";
            log.debug("Выбран метод Stream API для малого объема данных (<= {} элементов)",
                    SMALL_LIST_THRESHOLD);
            result = mergeLists(invoiceList, productionList);

        } else if (totalSize <= MEDIUM_LIST_THRESHOLD) {
            selectedMethod = "Оптимизированный ручной";
            log.debug("Выбран оптимизированный ручной метод для среднего объема данных ({} - {} элементов)",
                    SMALL_LIST_THRESHOLD + 1, MEDIUM_LIST_THRESHOLD);
            result = mergeListsOptimized(invoiceList, productionList);

        } else {
            selectedMethod = "Параллельный";
            log.debug("Выбран параллельный метод для большого объема данных (> {} элементов)",
                    MEDIUM_LIST_THRESHOLD);
            result = mergeListsParallel(invoiceList, productionList);
        }

        long endTime = System.currentTimeMillis();
        log.info("Автовыбор завершен. Выбран метод: {}, Размер результата: {}, Общее время: {} мс",
                selectedMethod, result.size(), (endTime - startTime));

        return result;
    }

    /**
     * Объединяет два списка в один с сортировкой по дате (убывание)
     * Использует Stream API для оптимальной производительности
     */
    private List<ResultingIncomeFullEntity> mergeLists(
            List<InvoiceFullEntity> invoiceList,
            List<ProductionFullEntity> productionList) {

        long startTime = System.currentTimeMillis();
        log.info("Начало объединения списков. Invoice: {}, Production: {}",
                invoiceList.size(), productionList.size());

        List<ResultingIncomeFullEntity> result = Stream.concat(
                        invoiceList.stream().map(this::convertInvoiceToResulting),
                        productionList.stream().map(this::convertProductionToResulting)
                )
                .sorted(Comparator.comparing(ResultingIncomeFullEntity::getDate).reversed())
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("Объединение завершено. Размер результата: {}, Время выполнения: {} мс",
                result.size(), (endTime - startTime));

        return result;
    }

    /**
     * Альтернативный метод с ручным объединением для максимальной производительности
     * при работе с очень большими списками (10k+ элементов)
     */
    private List<ResultingIncomeFullEntity> mergeListsOptimized(
            List<InvoiceFullEntity> invoiceList,
            List<ProductionFullEntity> productionList) {

        long startTime = System.currentTimeMillis();
        log.info("Начало оптимизированного объединения. Invoice: {}, Production: {}",
                invoiceList.size(), productionList.size());

        int totalSize = invoiceList.size() + productionList.size();
        List<ResultingIncomeFullEntity> result = new ArrayList<>(totalSize);

        // Ручное добавление без создания промежуточных потоков
        for (InvoiceFullEntity entity : invoiceList) {
            result.add(convertInvoiceToResulting(entity));
        }

        for (ProductionFullEntity entity : productionList) {
            result.add(convertProductionToResulting(entity));
        }

        // Быстрая сортировка с пользовательским компаратором
        result.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));

        long endTime = System.currentTimeMillis();
        log.info("Оптимизированное объединение завершено. Размер: {}, Время: {} мс",
                result.size(), (endTime - startTime));

        return result;
    }

    /**
     * Конвертация Invoice в Resulting со всеми полями
     */
    private ResultingIncomeFullEntity convertInvoiceToResulting(InvoiceFullEntity invoice) {
        ResultingIncomeFullEntity resulting = new ResultingIncomeFullEntity();

        resulting.setDate(invoice.getDate());
        resulting.setDocumentType(invoice.getDocumentType());
        resulting.setDocumentTypeOneC(invoice.getDocumentTypeOneC());
        resulting.setNumber(invoice.getNumber());
        resulting.setRefKey(invoice.getRefKey());
        resulting.setOrganizationKey(invoice.getOrganizationKey());
        resulting.setStructuralUnitKey(invoice.getStructuralUnitKey());
        resulting.setCustomerOrder(invoice.getCustomerOrder());
        resulting.setNomenclatureKey(invoice.getNomenclatureKey());
        resulting.setCharacteristicKey(invoice.getCharacteristicKey());
        resulting.setBatchKey(invoice.getBatchKey());
        resulting.setQuantity(invoice.getQuantity());
        resulting.setPrice(invoice.getPrice());

        return resulting;
    }

    /**
     * Конвертация Production в Resulting со всеми полями
     */
    private ResultingIncomeFullEntity convertProductionToResulting(ProductionFullEntity production) {
        ResultingIncomeFullEntity resulting = new ResultingIncomeFullEntity();

        resulting.setDate(production.getDate());
        resulting.setDocumentType(production.getDocumentType());
        resulting.setDocumentTypeOneC(production.getDocumentTypeOneC());
        resulting.setNumber(production.getNumber());
        resulting.setRefKey(production.getRefKey());
        resulting.setOrganizationKey(production.getOrganizationKey());
        resulting.setStructuralUnitKey(production.getStructuralUnitKey());
        resulting.setCustomerOrder(production.getCustomerOrder());
        resulting.setNomenclatureKey(production.getNomenclatureKey());
        resulting.setCharacteristicKey(production.getCharacteristicKey());
        resulting.setBatchKey(production.getBatchKey());
        resulting.setQuantity(production.getQuantity());
        resulting.setPrice(production.getPrice());

        return resulting;
    }

    /**
     * Параллельная обработка для очень больших списков (100k+ элементов)
     */
    private List<ResultingIncomeFullEntity> mergeListsParallel(
            List<InvoiceFullEntity> invoiceList,
            List<ProductionFullEntity> productionList) {

        long startTime = System.currentTimeMillis();
        log.info("Начало параллельного объединения. Invoice: {}, Production: {}",
                invoiceList.size(), productionList.size());

        List<ResultingIncomeFullEntity> result = Stream.concat(
                        invoiceList.parallelStream().map(this::convertInvoiceToResulting),
                        productionList.parallelStream().map(this::convertProductionToResulting)
                )
                .parallel()
                .sorted(Comparator.comparing(ResultingIncomeFullEntity::getDate).reversed())
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("Параллельное объединение завершено. Размер: {}, Время: {} мс",
                result.size(), (endTime - startTime));

        return result;
    }
}
