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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final RestClientConfig restClientConfig;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceRepository invoiceRepository;

    private static final int BATCH_SIZE = 500;
    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private static final int REQUEST_DELAY_MS = 20;

    @Override
    public Page<InvoiceEntity> getAllInvoice(
            UUID organizationId,
            LocalDateTime endDate
    ) {
        log.info("-------> InvoiceServiceImpl -------> getAllInvoice");

        invoiceRepository.deleteAll();

        AtomicBoolean hasMoreData = new AtomicBoolean(true);
        int skip = 0;

        // Создаем пул потоков для параллельных запросов
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);

        // Очередь для результатов
        BlockingQueue<List<InvoiceItemResponseDto>> resultsQueue = new LinkedBlockingQueue<>();

        // Счетчик активных задач
        AtomicInteger activeTasks = new AtomicInteger(0);

        try {
            while (hasMoreData.get()) {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                // Запускаем MAX_CONCURRENT_REQUESTS параллельных запросов
                for (int i = 0; i < MAX_CONCURRENT_REQUESTS; i++) {
                    final int currentSkip = skip;
                    skip += BATCH_SIZE;

                    activeTasks.incrementAndGet();

                    CompletableFuture<Void> future = CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    log.info("------> Выполняется запрос с skip={}", currentSkip);

                                    InvoiceResponseDto response = getInvoice(
                                            organizationId,
                                            endDate,
                                            BATCH_SIZE,
                                            currentSkip
                                    );

                                    List<InvoiceItemResponseDto> items = response.getValue();

                                    if (!items.isEmpty()) {
                                        resultsQueue.put(items);
                                        log.info("------> Получено {} записей с skip={}", items.size(), currentSkip);
                                    } else {
                                        log.info("------> Пустой ответ с skip={}, прекращаем загрузку", currentSkip);
                                        hasMoreData.set(false);
                                    }

                                    return items;

                                } catch (Exception e) {
                                    log.error("Ошибка при выполнении запроса с skip={}: {}", currentSkip, e.getMessage());
                                    throw new CompletionException(e);
                                } finally {
                                    activeTasks.decrementAndGet();
                                }
                            }, executorService)
                            .thenAccept(items -> {
                                // Сохраняем полученные данные
                                if (!items.isEmpty()) {
                                    try {
                                        for (InvoiceItemResponseDto value : items) {
                                            invoiceRepository.save(invoiceMapper.toEntity(value));
                                        }
                                        log.info("------> Сохранено {} записей с skip={}", items.size(), currentSkip);
                                    } catch (DataIntegrityViolationException e) {
                                        log.error("Ошибка целостности данных: {}", e.getMessage());
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
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Ошибка при выполнении партии запросов: {}", e.getMessage());
                    // Отменяем незавершенные задачи
                    futures.forEach(f -> f.cancel(true));
                }
            }

            // Ждем завершения всех оставшихся задач
            while (activeTasks.get() > 0) {
                TimeUnit.MILLISECONDS.sleep(100);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("------> Все приходники из 1с найдены и сохранены в базу");
        return invoiceRepository.findAll(PageRequest.of(0, 10));
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