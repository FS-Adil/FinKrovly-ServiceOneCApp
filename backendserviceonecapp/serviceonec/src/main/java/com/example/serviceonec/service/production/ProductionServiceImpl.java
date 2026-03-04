package com.example.serviceonec.service.production;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryResponseDto;
import com.example.serviceonec.model.dto.response.production.ProductionItemResponseDto;
import com.example.serviceonec.model.dto.response.production.ProductionResponseDto;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import com.example.serviceonec.model.entity.production.ProductionStocksEntity;
import com.example.serviceonec.model.mapper.production.ProductionDestributionStocksMapper;
import com.example.serviceonec.model.mapper.production.ProductionItemsMapper;
import com.example.serviceonec.model.mapper.production.ProductionMapper;
import com.example.serviceonec.model.mapper.production.ProductionStocksMapper;
import com.example.serviceonec.repository.production.ProductionDistributionStocksRepository;
import com.example.serviceonec.repository.production.ProductionItemsRepository;
import com.example.serviceonec.repository.production.ProductionRepository;
import com.example.serviceonec.repository.production.ProductionStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionServiceImpl implements ProductionService {

    private final RestClientConfig restClientConfig;

    private final ProductionRepository productionRepository;
    private final ProductionStocksRepository productionStocksRepository;
    private final ProductionItemsRepository productionItemsRepository;
    private final ProductionDistributionStocksRepository productionDistributionStocksRepository;

    private final ProductionMapper productionMapper;
    private final ProductionStocksMapper productionStocksMapper;
    private final ProductionItemsMapper productionItemsMapper;
    private final ProductionDestributionStocksMapper productionDestributionStocksMapper;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    private static final int REQUEST_DELAY_MS = 20;

    @Override
    public Page<ProductionEntity> getAllProduction(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        log.info("===== НАЧАЛО ЗАГРУЗКИ ПРОИЗВОДСТВ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Период: {} - {}",
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("Параметры загрузки: batchSize={}, maxConcurrentRequests={}", BATCH_SIZE, MAX_CONCURRENT_REQUESTS);

        // Очистка таблиц перед загрузкой
        log.info("Очистка таблиц производств...");
        long cleanupStart = System.currentTimeMillis();

        productionDistributionStocksRepository.deleteAll();
        log.info("✓ Таблица production_distribution_stocks очищена");

        productionItemsRepository.deleteAll();
        log.info("✓ Таблица production_items очищена");

        productionStocksRepository.deleteAll();
        log.info("✓ Таблица production_stocks очищена");

        productionRepository.deleteAll();
        log.info("✓ Таблица productions очищена");

        log.info("Очистка таблиц завершена за {} мс", System.currentTimeMillis() - cleanupStart);

        AtomicBoolean hasMoreData = new AtomicBoolean(true);
        int skip = 0;
        AtomicLong totalDocumentsLoaded = new AtomicLong(0);
        AtomicLong totalStocksLoaded = new AtomicLong(0);
        AtomicLong totalItemsLoaded = new AtomicLong(0);
        AtomicLong totalDistributionStocksLoaded = new AtomicLong(0);
        AtomicInteger batchCounter = new AtomicInteger(0);
        AtomicInteger activeTasks = new AtomicInteger(0);

        // Создаем пул потоков для параллельных запросов
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
        log.info("Создан пул потоков на {} потоков", MAX_CONCURRENT_REQUESTS);

        long startTime = System.currentTimeMillis();

        try {
            while (hasMoreData.get()) {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                int currentBatch = batchCounter.incrementAndGet();

                log.info("--- ПАРТИЯ #{}: запуск {} параллельных запросов ---",
                        currentBatch, MAX_CONCURRENT_REQUESTS);

                // Запускаем MAX_CONCURRENT_REQUESTS параллельных запросов
                for (int i = 0; i < MAX_CONCURRENT_REQUESTS; i++) {
                    final int currentSkip = skip;
                    final int requestNumber = i + 1;
                    skip += BATCH_SIZE;

                    activeTasks.incrementAndGet();

                    CompletableFuture<Void> future = CompletableFuture
                            .supplyAsync(() -> {
                                long requestStartTime = System.currentTimeMillis();
                                String threadName = Thread.currentThread().getName();

                                log.debug("[Поток: {}] Запрос #{}.{}: skip={}, top={}",
                                        threadName, currentBatch, requestNumber, currentSkip, BATCH_SIZE);

                                try {
                                    ProductionResponseDto productionResponseDto = getResponse(
                                            organizationId,
                                            BATCH_SIZE,
                                            currentSkip,
                                            startDate,
                                            endDate
                                    );

                                    List<ProductionItemResponseDto> items = productionResponseDto.getValue();
                                    long requestTime = System.currentTimeMillis() - requestStartTime;

                                    if (!items.isEmpty()) {
                                        log.info("[Поток: {}] Запрос #{}.{} УСПЕШНО: получено {} документов (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, items.size(), currentSkip, requestTime);
                                        return items;
                                    } else {
                                        log.info("[Поток: {}] Запрос #{}.{} ЗАВЕРШЕН: данных нет (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, currentSkip, requestTime);
                                        hasMoreData.set(false);
                                        return new ArrayList<ProductionItemResponseDto>();
                                    }

                                } catch (Exception e) {
                                    log.error("[Поток: {}] Запрос #{}.{} ОШИБКА: skip={}, ошибка: {}",
                                            threadName, currentBatch, requestNumber, currentSkip, e.getMessage());
                                    throw new CompletionException(e);
                                } finally {
                                    activeTasks.decrementAndGet();
                                }
                            }, executorService)
                            .thenAccept(items -> {
                                // Сохраняем полученные данные
                                if (!items.isEmpty()) {
                                    long saveStartTime = System.currentTimeMillis();
                                    try {
                                        int docsSaved = 0;
                                        int stocksSaved = 0;
                                        int prodItemsSaved = 0;
                                        int distStocksSaved = 0;

                                        for (ProductionItemResponseDto value : items) {
                                            try {
                                                // Сохраняем основной документ
                                                UUID refKey = value.getRefKey();
                                                productionRepository.save(productionMapper.toEntity(value));
                                                docsSaved++;

                                                // Сохраняем запасы
                                                if (value.getStocks() != null) {
                                                    for (ProductionItemResponseDto.ProductionStocksDto stock : value.getStocks()) {
                                                        productionStocksRepository.save(productionStocksMapper.toEntity(stock));
                                                        stocksSaved++;
                                                    }
                                                }

                                                // Сохраняем продукцию
                                                if (value.getProducts() != null) {
                                                    for (ProductionItemResponseDto.ProductionItemsDto item : value.getProducts()) {
                                                        productionItemsRepository.save(productionItemsMapper.toEntity(item));
                                                        prodItemsSaved++;
                                                    }
                                                }

                                                // Сохраняем распределение запасов
                                                if (value.getDistributionStocks() != null) {
                                                    for (ProductionItemResponseDto.ProductionDistributionStocksDto item : value.getDistributionStocks()) {
                                                        productionDistributionStocksRepository.save(productionDestributionStocksMapper.toEntity(item));
                                                        distStocksSaved++;
                                                    }
                                                }

                                            } catch (DataIntegrityViolationException e) {
                                                log.error("❌ Ошибка целостности данных для документа {}: {}",
                                                        value.getRefKey(), e.getMessage());
                                            }
                                        }

                                        // Обновляем счетчики
                                        totalDocumentsLoaded.addAndGet(docsSaved);
                                        totalStocksLoaded.addAndGet(stocksSaved);
                                        totalItemsLoaded.addAndGet(prodItemsSaved);
                                        totalDistributionStocksLoaded.addAndGet(distStocksSaved);

                                        long saveTime = System.currentTimeMillis() - saveStartTime;
                                        log.info("💾 [Поток: {}] Сохранено: {} документов, {} запасов, {} продукции, {} распределений (время: {} мс)",
                                                Thread.currentThread().getName(), docsSaved, stocksSaved,
                                                prodItemsSaved, distStocksSaved, saveTime);

                                    } catch (Exception e) {
                                        log.error("❌ [Поток: {}] Критическая ошибка при сохранении: {}",
                                                Thread.currentThread().getName(), e.getMessage());
                                    }
                                }
                            });

                    futures.add(future);

                    // Небольшая задержка между запуском запросов
                    try {
                        TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }

                // Ждем завершения всех запросов в текущей партии
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                try {
                    allFutures.get(30, TimeUnit.SECONDS);

                    // Логируем прогресс после каждой партии
                    log.info("✅ ПАРТИЯ #{} полностью завершена", currentBatch);
                    log.info("📊 ПРОГРЕСС: всего документов: {}, запасов: {}, продукции: {}, распределений: {}",
                            totalDocumentsLoaded.get(), totalStocksLoaded.get(),
                            totalItemsLoaded.get(), totalDistributionStocksLoaded.get());

                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("❌ Ошибка при выполнении партии #{}: {}", currentBatch, e.getMessage());
                    // Отменяем незавершенные задачи
                    futures.forEach(f -> f.cancel(true));
                }
            }

            // Ждем завершения всех оставшихся задач
            log.info("Ожидание завершения всех задач...");
            while (activeTasks.get() > 0) {
                log.debug("Активных задач: {}", activeTasks.get());
                TimeUnit.MILLISECONDS.sleep(100);
            }

        } catch (InterruptedException e) {
            log.error("❌ Ошибка при ожидании завершения задач: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;

            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Принудительное завершение потоков");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ ПРОИЗВОДСТВ =====");
            log.info("✅ ИТОГОВАЯ СТАТИСТИКА:");
            log.info("   📄 Документов производств: {}", totalDocumentsLoaded.get());
            log.info("   📦 Запасов: {}", totalStocksLoaded.get());
            log.info("   🏭 Продукции: {}", totalItemsLoaded.get());
            log.info("   📊 Распределений запасов: {}", totalDistributionStocksLoaded.get());
            log.info("   📈 ВСЕГО ЗАПИСЕЙ: {}",
                    totalDocumentsLoaded.get() + totalStocksLoaded.get() +
                            totalItemsLoaded.get() + totalDistributionStocksLoaded.get());
            log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);

            if (totalTime > 0) {
                long totalRecords = totalDocumentsLoaded.get() + totalStocksLoaded.get() +
                        totalItemsLoaded.get() + totalDistributionStocksLoaded.get();
                log.info("📊 Средняя скорость: {} записей/сек",
                        totalRecords / (totalTime / 1000 > 0 ? totalTime / 1000 : 1));
            }
        }

        log.info("------> Все Производства из 1с за период с {} по {} найдены и сохранены в базу",
                startDate, endDate);

        Page<ProductionEntity> result = productionRepository.findAll(PageRequest.of(0, 10));
        log.info("📄 Возвращаем первые {} записей производств из {} всего",
                result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    @Override
    public Page<ProductionItemsEntity> getAllProductionItems() {
        log.warn("Метод getAllProductionItems() вызван, но не реализован");
        return null;
    }

    @Override
    public Page<ProductionStocksEntity> getAllProductionStocks() {
        log.warn("Метод getAllProductionStocks() вызван, но не реализован");
        return null;
    }

    @Override
    public void getAllProductionByCustomerOrders(List<UUID> missingCustomerOrderKeys) {
        log.info("===== НАЧАЛО ЗАГРУЗКИ НЕ ДОСТАЮЩИХ ПРОИЗВОДСТВ =====");
        log.info("Всего заказов для обработки: {}", missingCustomerOrderKeys.size());

        if (missingCustomerOrderKeys == null || missingCustomerOrderKeys.isEmpty()) {
            log.warn("Список заказов пуст");
            return;
        }

        // Создаем фиксированный пул потоков с 3 потоками
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        // Список для отслеживания завершения задач
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Счетчики для отслеживания прогресса
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFailed = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);

        // Разбиваем список на части для каждого потока
        int chunkSize = (int) Math.ceil((double) missingCustomerOrderKeys.size() / 3);
        log.info("Размер чанка для каждого потока: {}", chunkSize);

        for (int i = 0; i < 3; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, missingCustomerOrderKeys.size());
            int threadNumber = i + 1;

            if (start >= missingCustomerOrderKeys.size()) {
                log.info("Поток {} пропущен (нет данных)", threadNumber);
                break;
            }

            List<UUID> chunk = missingCustomerOrderKeys.subList(start, end);
            log.info("Поток {} получил чанк с {} по {} (всего {} заказов)",
                    threadNumber, start, end - 1, chunk.size());

            // Создаем асинхронную задачу для каждого чанка
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processCustomerOrdersChunk(chunk, threadNumber, totalProcessed, totalSuccess, totalFailed, totalSkipped);
            }, executorService);

            futures.add(future);
        }

        // Ожидаем завершения всех задач
        try {
            log.info("Ожидание завершения всех потоков...");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Логируем итоговую статистику
            log.info("===== ИТОГОВАЯ СТАТИСТИКА =====");
            log.info("Всего обработано заказов: {}", totalProcessed.get());
            log.info("✅ Успешно: {}", totalSuccess.get());
            log.info("❌ С ошибками: {}", totalFailed.get());
            log.info("⏭️ Пропущено (нет данных): {}", totalSkipped.get());

        } catch (Exception e) {
            log.error("❌ Ошибка при выполнении многопоточной загрузки: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // Корректно завершаем executor service
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Принудительное завершение потоков");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Прерывание при завершении потоков");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ НЕ ДОСТАЮЩИХ ПРОИЗВОДСТВ =====");
    }

    private void processCustomerOrdersChunk(List<UUID> customerOrderKeys, int threadNumber,
                                            AtomicInteger totalProcessed, AtomicInteger totalSuccess,
                                            AtomicInteger totalFailed, AtomicInteger totalSkipped) {
        String threadName = Thread.currentThread().getName();
        log.info("[Поток {}: {}] НАЧАЛО обработки {} заказов", threadNumber, threadName, customerOrderKeys.size());

        int processedInThread = 0;
        int successInThread = 0;
        int failedInThread = 0;
        int skippedInThread = 0;

        for (int i = 0; i < customerOrderKeys.size(); i++) {
            UUID uuid = customerOrderKeys.get(i);
            int itemNumber = i + 1;

            try {
                log.debug("[Поток {}] Обработка заказа {}/{}: {}",
                        threadNumber, itemNumber, customerOrderKeys.size(), uuid);

                ProductionResponseDto productionResponseDto = getResponseByMissingCustomerOrder(uuid);
                processedInThread++;
                totalProcessed.incrementAndGet();

                if (productionResponseDto != null && productionResponseDto.getValue() != null) {
                    int itemsCount = productionResponseDto.getValue().size();
                    log.info("[Поток {}] Заказ {}: найдено {} производственных документов",
                            threadNumber, uuid, itemsCount);

                    if (itemsCount > 0) {
                        processProductionItems(productionResponseDto.getValue(), threadNumber, uuid);
                        successInThread++;
                        totalSuccess.incrementAndGet();
                        log.info("[Поток {}] ✅ Заказ {} успешно обработан ({} документов)",
                                threadNumber, uuid, itemsCount);
                    } else {
                        skippedInThread++;
                        totalSkipped.incrementAndGet();
                        log.info("[Поток {}] ⏭️ Заказ {}: нет производственных документов (пропущен)",
                                threadNumber, uuid);
                    }
                } else {
                    skippedInThread++;
                    totalSkipped.incrementAndGet();
                    log.info("[Поток {}] ⏭️ Заказ {}: нет данных от 1С (пропущен)", threadNumber, uuid);
                }

            } catch (Exception e) {
                failedInThread++;
                totalFailed.incrementAndGet();
                log.error("[Поток {}] ❌ Ошибка при обработке заказа {}: {}",
                        threadNumber, uuid, e.getMessage());
                log.debug("[Поток {}] Детали ошибки: ", threadNumber, e);
            }

            // Логируем прогресс каждые 10 заказов
            if (itemNumber % 10 == 0 || itemNumber == customerOrderKeys.size()) {
                log.info("[Поток {}] Прогресс: {}/{} заказов (✅ {} успешно, ❌ {} ошибок, ⏭️ {} пропущено)",
                        threadNumber, itemNumber, customerOrderKeys.size(),
                        successInThread, failedInThread, skippedInThread);
            }
        }

        log.info("[Поток {}: {}] ЗАВЕРШЕНИЕ. Итого в потоке: обработано {}, ✅ успешно {}, ❌ ошибок {}, ⏭️ пропущено {}",
                threadNumber, threadName, processedInThread, successInThread, failedInThread, skippedInThread);
    }

    private void processProductionItems(List<ProductionItemResponseDto> items, int threadNumber, UUID parentOrderUuid) {
        log.debug("[Поток {}] Начало сохранения {} производственных документов для заказа {}",
                threadNumber, items.size(), parentOrderUuid);

        int savedItems = 0;
        int failedItems = 0;

        for (ProductionItemResponseDto value : items) {
            try {
                // Сохраняем основной документ
                UUID refKey = value.getRefKey();
                productionRepository.save(productionMapper.toEntity(value));
                log.debug("[Поток {}] Сохранен основной документ: {}", threadNumber, refKey);

                // Сохраняем запасы
                if (value.getStocks() != null && !value.getStocks().isEmpty()) {
                    int stocksCount = 0;
                    for (ProductionItemResponseDto.ProductionStocksDto stock : value.getStocks()) {
                        productionStocksRepository.save(productionStocksMapper.toEntity(stock));
                        stocksCount++;
                    }
                    log.debug("[Поток {}] Сохранено {} записей запасов для документа {}",
                            threadNumber, stocksCount, refKey);
                }

                // Сохраняем продукцию
                if (value.getProducts() != null && !value.getProducts().isEmpty()) {
                    int productsCount = 0;
                    for (ProductionItemResponseDto.ProductionItemsDto item : value.getProducts()) {
                        productionItemsRepository.save(productionItemsMapper.toEntity(item));
                        productsCount++;
                    }
                    log.debug("[Поток {}] Сохранено {} записей продукции для документа {}",
                            threadNumber, productsCount, refKey);
                }

                // Сохраняем распределение запасов
                if (value.getDistributionStocks() != null && !value.getDistributionStocks().isEmpty()) {
                    int distributionCount = 0;
                    for (ProductionItemResponseDto.ProductionDistributionStocksDto item : value.getDistributionStocks()) {
                        productionDistributionStocksRepository.save(productionDestributionStocksMapper.toEntity(item));
                        distributionCount++;
                    }
                    log.debug("[Поток {}] Сохранено {} записей распределения запасов для документа {}",
                            threadNumber, distributionCount, refKey);
                }

                savedItems++;

            } catch (DataIntegrityViolationException e) {
                failedItems++;
                log.error("[Поток {}] ❌ Ошибка целостности данных для документа {}: {}",
                        threadNumber, value.getRefKey(), e.getMessage());
            } catch (Exception e) {
                failedItems++;
                log.error("[Поток {}] ❌ Неожиданная ошибка при сохранении документа {}: {}",
                        threadNumber, value.getRefKey(), e.getMessage());
            }
        }

        log.debug("[Поток {}] Завершено сохранение документов для заказа {}: сохранено {}, ошибок {}",
                threadNumber, parentOrderUuid, savedItems, failedItems);
    }

    private ProductionResponseDto getResponseByMissingCustomerOrder(UUID customerOrderKeys) {
        String url = String.format("/Document_СборкаЗапасов?" +
                        "$filter=Posted eq true" +
                        " and ЗаказПокупателя_Key eq guid'%s'" +
                        "&" +
                        "$select=Ref_Key, Number, Date, ЗаказПокупателя_Key, Организация_Key, Продукция, Запасы, РаспределениеЗапасов&" +
                        "$format=json",
                customerOrderKeys
        );

        log.debug("URL запроса для заказа {}: {}", customerOrderKeys, url.replaceAll("['\"]", ""));
        long startTime = System.currentTimeMillis();

        try {
            ProductionResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ProductionResponseDto.class);

            long duration = System.currentTimeMillis() - startTime;

            if (response != null && response.getValue() != null) {
                log.debug("Заказ {}: получено {} записей за {} мс",
                        customerOrderKeys, response.getValue().size(), duration);
            } else {
                log.debug("Заказ {}: получен пустой ответ за {} мс", customerOrderKeys, duration);
            }

            return response;

        } catch (Exception e) {
            log.error("Ошибка при получении Производств для заказа {}: {}",
                    customerOrderKeys, e.getMessage());
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    private ProductionResponseDto getResponse(
            UUID organizationId,
            Integer top,
            Integer skip,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        String url = String.format("/Document_СборкаЗапасов?" +
                        "$filter=Posted eq true" +
                        " and Организация_Key eq guid'%s'" +
                        " and Date ge datetime'%s'" +
                        " and Date le datetime'%s'" +
                        "&" +
                        "$select=Ref_Key, Number, Date, ЗаказПокупателя_Key, Организация_Key, Продукция, Запасы, РаспределениеЗапасов&" +
                        "$orderby=Date desc&" +
                        "$top=%d&$skip=%d&" +
                        "$format=json",
                organizationId,
                startDate,
                endDate,
                top,
                skip);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            ProductionResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ProductionResponseDto.class);

            return response;

        } catch (Exception e) {
            log.error("Ошибка при получении Производств с skip={}: {}", skip, e.getMessage());
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }
}