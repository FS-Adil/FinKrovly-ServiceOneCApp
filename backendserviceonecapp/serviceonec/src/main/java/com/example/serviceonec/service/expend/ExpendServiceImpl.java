package com.example.serviceonec.service.expend;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.expend.ExpendItemResponseDto;
import com.example.serviceonec.model.dto.response.expend.ExpendResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.mapper.expend.ExpendMapper;
import com.example.serviceonec.repository.expend.ExpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

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
public class ExpendServiceImpl implements ExpendService {

    private final RestClientConfig restClientConfig;
    private final ExpendRepository expendRepository;
    private final ExpendMapper expendMapper;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private static final int REQUEST_DELAY_MS = 50;
    private static final int REQUEST_TIMEOUT_SECONDS = 60;
    private static final int MAX_EMPTY_BATCHES = 3;

    @Override
    public Page<ExpendEntity> getAllExpend(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        log.info("===== НАЧАЛО ЗАГРУЗКИ РАСХОДНЫХ НАКЛАДНЫХ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Период: {} - {}",
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("Параметры загрузки: batchSize={}, maxConcurrentRequests={}", BATCH_SIZE, MAX_CONCURRENT_REQUESTS);

        // Очистка таблицы перед загрузкой
        log.info("Очистка таблицы расходных накладных...");
        expendRepository.deleteAll();
        log.info("Таблица очищена");

        AtomicBoolean hasError = new AtomicBoolean(false);
        AtomicBoolean shouldContinue = new AtomicBoolean(true);
        int skip = 0;
        AtomicLong totalRecordsLoaded = new AtomicLong(0);
        AtomicInteger batchCounter = new AtomicInteger(0);
        AtomicInteger consecutiveEmptyBatches = new AtomicInteger(0);

        // Создаем пул потоков для параллельных запросов
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
        log.info("Создан пул потоков на {} потоков", MAX_CONCURRENT_REQUESTS);

        // Счетчик активных задач
        AtomicInteger activeTasks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        try {
            while (shouldContinue.get() && !hasError.get()) {
                // Проверка на слишком много пустых батчей подряд
                if (consecutiveEmptyBatches.get() >= MAX_EMPTY_BATCHES) {
                    log.info("Получено {} пустых батчей подряд. Завершаем загрузку.", MAX_EMPTY_BATCHES);
                    break;
                }

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                int currentBatch = batchCounter.incrementAndGet();

                log.info("--- ПАРТИЯ #{}: запуск {} параллельных запросов ---",
                        currentBatch, MAX_CONCURRENT_REQUESTS);

                // Используем AtomicBoolean для отслеживания наличия данных в батче
                AtomicBoolean batchHasData = new AtomicBoolean(false);

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
                                    ExpendResponseDto expendResponseDto = getExpend(
                                            organizationId,
                                            startDate,
                                            endDate,
                                            BATCH_SIZE,
                                            currentSkip
                                    );

                                    List<ExpendItemResponseDto> items = expendResponseDto != null ?
                                            expendResponseDto.getValue() : new ArrayList<>();

                                    long requestTime = System.currentTimeMillis() - requestStartTime;

                                    if (!items.isEmpty()) {
                                        log.debug("[Поток: {}] Запрос #{}.{} УСПЕШНО: получено {} записей (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, items.size(), currentSkip, requestTime);

                                        // Отмечаем, что в батче есть данные
                                        batchHasData.set(true);

                                        return items;
                                    } else {
                                        log.debug("[Поток: {}] Запрос #{}.{}: данных нет (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, currentSkip, requestTime);
                                        return new ArrayList<ExpendItemResponseDto>();
                                    }

                                } catch (HttpClientErrorException.NotFound e) {
                                    // 404 - данные закончились
                                    log.debug("[Поток: {}] Запрос #{}.{}: данные закончились (404) skip={}",
                                            threadName, currentBatch, requestNumber, currentSkip);
                                    return new ArrayList<ExpendItemResponseDto>();
                                } catch (HttpClientErrorException e) {
                                    log.error("[Поток: {}] Запрос #{}.{} HTTP ошибка {}: skip={}, ошибка: {}",
                                            threadName, currentBatch, requestNumber,
                                            e.getStatusCode(), currentSkip, e.getMessage());
                                    throw new CompletionException(e);
                                } catch (ResourceAccessException e) {
                                    log.error("[Поток: {}] Запрос #{}.{} ТАЙМАУТ: skip={}, ошибка: {}",
                                            threadName, currentBatch, requestNumber, currentSkip, e.getMessage());
                                    throw new CompletionException(e);
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
                                        List<ExpendEntity> entities = new ArrayList<>();
                                        for (ExpendItemResponseDto value : items) {
                                            entities.add(expendMapper.toEntity(value));
                                        }

                                        // Сохраняем батчем для производительности
                                        expendRepository.saveAll(entities);
                                        int savedCount = entities.size();

                                        totalRecordsLoaded.addAndGet(savedCount);

                                        long saveTime = System.currentTimeMillis() - saveStartTime;
                                        log.info("💾 Сохранено {} записей (всего: {}, время сохранения: {} мс)",
                                                savedCount, totalRecordsLoaded.get(), saveTime);

                                    } catch (DataIntegrityViolationException e) {
                                        log.error("❌ Ошибка целостности данных при сохранении: {}", e.getMessage());
                                    }
                                }
                            })
                            .exceptionally(throwable -> {
                                log.error("❌ Необработанная ошибка в запросе #{}.{}: {}",
                                        currentBatch, requestNumber, throwable.getMessage());
                                hasError.set(true);
                                return null;
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
                    allFutures.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    // Проверяем, были ли данные в этой партии
                    if (!batchHasData.get()) {
                        consecutiveEmptyBatches.incrementAndGet();
                        log.info("⚠️ ПАРТИЯ #{} не содержит данных. Пустых батчей подряд: {}",
                                currentBatch, consecutiveEmptyBatches.get());
                    } else {
                        consecutiveEmptyBatches.set(0);
                        log.info("✅ ПАРТИЯ #{} полностью завершена, получены данные", currentBatch);
                    }

                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("❌ Ошибка при выполнении партии #{}: {}", currentBatch, e.getMessage());
                    hasError.set(true);

                    // Отменяем незавершенные задачи
                    futures.forEach(f -> f.cancel(true));

                    // Если ошибка - прекращаем выполнение
                    break;
                }

                // Небольшая пауза между батчами
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Ждем завершения всех оставшихся задач
            log.info("Ожидание завершения всех задач...");
            int waitAttempts = 0;
            while (activeTasks.get() > 0 && waitAttempts < 30) {
                log.debug("Активных задач: {}", activeTasks.get());
                TimeUnit.MILLISECONDS.sleep(100);
                waitAttempts++;
            }

        } catch (InterruptedException e) {
            log.error("❌ Ошибка при ожидании завершения задач: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;

            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Принудительное завершение потоков");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ =====");
            if (hasError.get()) {
                log.error("❌ Загрузка завершилась с ошибками");
            }
            log.info("✅ Всего загружено записей: {}", totalRecordsLoaded.get());
            log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
            if (totalTime > 0) {
                log.info("📊 Средняя скорость: {} записей/сек",
                        totalRecordsLoaded.get() / (totalTime / 1000));
            }
        }

        log.info("------> Все расходники из 1с за период с {} по {} обработаны", startDate, endDate);

        Page<ExpendEntity> result = expendRepository.findAll(PageRequest.of(0, 10));
        log.info("📄 Возвращаем первые {} записей из {} всего",
                result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    private ExpendResponseDto getExpend(
            UUID organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer top,
            Integer skip
    ) {
        String url = String.format("/Document_РасходнаяНакладная?" +
                "$filter=Posted eq true" +
                " and Организация_Key eq guid'" + organizationId + "'" +
                " and Date ge datetime'" + startDate + "'" +
                " and Date le datetime'" + endDate + "'" +
                "&" +
                "$select=Number,Date,Организация_Key,ВидОперации,Ref_Key,СтруктурнаяЕдиница_Key,Заказ&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Date desc&" +
                "$format=json", top, skip);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            ExpendResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendResponseDto.class);

            if (response == null) {
                log.warn("Получен пустой ответ от 1С для skip={}", skip);
                return new ExpendResponseDto();
            }

            return response;

        } catch (HttpClientErrorException.NotFound e) {
            // 404 - это нормально, значит данные закончились
            log.debug("Данные закончились (404) для skip={}", skip);
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении Расходных накладных с skip={}: {}", skip, e.getMessage());
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }
}