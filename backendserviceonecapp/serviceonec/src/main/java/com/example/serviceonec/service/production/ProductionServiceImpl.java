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