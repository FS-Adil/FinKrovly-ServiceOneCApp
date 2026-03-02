package com.example.serviceonec.service.invoice;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.invoice.InvoiceResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceItemResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import com.example.serviceonec.model.mapper.invoice.InvoiceMapper;
import com.example.serviceonec.repository.invoice.InvoiceRepository;
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
public class InvoiceServiceImpl implements InvoiceService {

    private final RestClientConfig restClientConfig;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceRepository invoiceRepository;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    private static final int REQUEST_DELAY_MS = 20;

    @Override
    public Page<InvoiceEntity> getAllInvoice(
            UUID organizationId,
            LocalDateTime endDate
    ) {
        log.info("===== НАЧАЛО ЗАГРУЗКИ ПРИХОДНЫХ НАКЛАДНЫХ =====");
        log.info("Организация ID: {}", organizationId);
        log.info("Период до: {}", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("Параметры загрузки: batchSize={}, maxConcurrentRequests={}", BATCH_SIZE, MAX_CONCURRENT_REQUESTS);

        // Очистка таблицы перед загрузкой
        log.info("Очистка таблицы приходных накладных...");
        invoiceRepository.deleteAll();
        log.info("Таблица очищена");

        AtomicBoolean hasMoreData = new AtomicBoolean(true);
        int skip = 0;
        AtomicLong totalRecordsLoaded = new AtomicLong(0);
        AtomicInteger batchCounter = new AtomicInteger(0);

        // Создаем пул потоков для параллельных запросов
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
        log.info("Создан пул потоков на {} потоков", MAX_CONCURRENT_REQUESTS);

        // Счетчик активных задач
        AtomicInteger activeTasks = new AtomicInteger(0);

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
                                    InvoiceResponseDto response = getInvoice(
                                            organizationId,
                                            endDate,
                                            BATCH_SIZE,
                                            currentSkip
                                    );

                                    List<InvoiceItemResponseDto> items = response.getValue();
                                    long requestTime = System.currentTimeMillis() - requestStartTime;

                                    if (!items.isEmpty()) {
                                        log.info("[Поток: {}] Запрос #{}.{} УСПЕШНО: получено {} записей (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, items.size(), currentSkip, requestTime);
                                        return items;
                                    } else {
                                        log.info("[Поток: {}] Запрос #{}.{} ЗАВЕРШЕН: данных нет (skip={}) за {} мс",
                                                threadName, currentBatch, requestNumber, currentSkip, requestTime);
                                        hasMoreData.set(false);
                                        return new ArrayList<InvoiceItemResponseDto>();
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
                                        int savedCount = 0;
                                        for (InvoiceItemResponseDto value : items) {
                                            invoiceRepository.save(invoiceMapper.toEntity(value));
                                            savedCount++;
                                        }
                                        totalRecordsLoaded.addAndGet(savedCount);

                                        long saveTime = System.currentTimeMillis() - saveStartTime;
                                        log.info("💾 Сохранено {} записей (всего: {}, время сохранения: {} мс)",
                                                savedCount, totalRecordsLoaded.get(), saveTime);

                                    } catch (DataIntegrityViolationException e) {
                                        log.error("❌ Ошибка целостности данных при сохранении: {}", e.getMessage());
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
                    log.info("✅ ПАРТИЯ #{} полностью завершена", currentBatch);
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

            log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ =====");
            log.info("✅ Всего загружено записей: {}", totalRecordsLoaded.get());
            log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
            log.info("📊 Средняя скорость: {} записей/сек",
                    totalRecordsLoaded.get() / (totalTime / 1000 > 0 ? totalTime / 1000 : 1));
        }

        log.info("------> Все приходники из 1с за период до {} найдены и сохранены в базу", endDate);

        Page<InvoiceEntity> result = invoiceRepository.findAll(PageRequest.of(0, 10));
        log.info("📄 Возвращаем первые {} записей из {} всего", result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    private InvoiceResponseDto getInvoice(
            UUID organizationId,
            LocalDateTime endDate,
            Integer top,
            Integer skip
    ) {
        String url = String.format("/Document_ПриходнаяНакладная?" +
                "$filter=Posted eq true" +
                " and Организация_Key eq guid'" + organizationId + "'" +
                " and Date le datetime'" + endDate + "'" +
                "&" +
                "$select= Number,Date,Ref_Key,Организация_Key,ВидОперации&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Date desc&" +
                "$format=json", top, skip);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            log.debug("------> Выполняется запрос к 1С с skip={}", skip);

            InvoiceResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceResponseDto.class);

            return response;

        } catch (Exception e) {
            log.error("Ошибка при получении Приходных накладных с skip={}: {}", skip, e.getMessage());
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }
}