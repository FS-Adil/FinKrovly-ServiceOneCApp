package com.example.serviceonec.service.invoice;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.model.mapper.invoice.InvoiceStocksMapper;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceStocksServiceImpl implements InvoiceStocksService {

    private final RestClientConfig restClientConfig;
    private final InvoiceStocksRepository invoiceStocksRepository;
    private final InvoiceStocksMapper invoiceStocksMapper;

    private static final int BATCH_SIZE = 500;
    private static final int REQUEST_DELAY_MS = 20;
    private static final int MAX_CONCURRENT_REQUESTS = 10;

    @Override
    public Page<InvoiceStocksEntity> getAllInvoiceStocks() {

        log.info("===== НАЧАЛО ЗАГРУЗКИ ВСЕХ ЗАПАСОВ ИЗ ПРИХОДНЫХ НАКЛАДНЫХ =====");
        log.info("Параметры загрузки: batchSize={}", BATCH_SIZE);

        // Очистка таблицы перед загрузкой
        log.info("Очистка таблицы запасов из приходных накладных...");
        invoiceStocksRepository.deleteAll();
        log.info("✅ Таблица очищена");

        boolean isStop = true;
        int skip = 0;
        int totalBatches = 0;
        AtomicLong totalRecordsLoaded = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        while (isStop) {
            totalBatches++;
            long batchStartTime = System.currentTimeMillis();

            log.info("--- ПАРТИЯ #{}: запрос с skip={}, top={} ---", totalBatches, skip, BATCH_SIZE);

            try {
                InvoiceStocksResponseDto invoiceStocksResponseDto = getInvoiceStocks(BATCH_SIZE, skip);

                List<InvoiceStocksItemResponseDto> items = invoiceStocksResponseDto.getValue();

                if (items.isEmpty()) {
                    log.info("📦 ПАРТИЯ #{}: пустой ответ, завершаем загрузку", totalBatches);
                    isStop = false;
                } else {
                    int batchSize = items.size();
                    long saveStartTime = System.currentTimeMillis();

                    int savedCount = 0;
                    for (InvoiceStocksItemResponseDto value : items) {
                        invoiceStocksRepository.save(invoiceStocksMapper.toEntity(value));
                        savedCount++;
                    }

                    totalRecordsLoaded.addAndGet(savedCount);
                    long saveTime = System.currentTimeMillis() - saveStartTime;
                    long batchTime = System.currentTimeMillis() - batchStartTime;

                    log.info("📦 ПАРТИЯ #{}: получено {} записей, сохранено {} (всего: {}, время сохранения: {} мс, общее время партии: {} мс)",
                            totalBatches, items.size(), savedCount, totalRecordsLoaded.get(), saveTime, batchTime);
                }

                skip += BATCH_SIZE;

                // Небольшая задержка между запросами
                try {
                    TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

            } catch (Exception e) {
                log.error("❌ Ошибка при обработке партии #{} с skip={}: {}", totalBatches, skip, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("===== ЗАВЕРШЕНИЕ ЗАГРУЗКИ =====");
        log.info("✅ Всего загружено записей: {}", totalRecordsLoaded.get());
        log.info("📊 Всего обработано партий: {}", totalBatches);
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("📈 Средняя скорость: {} записей/сек",
                totalRecordsLoaded.get() / (totalTime / 1000 > 0 ? totalTime / 1000 : 1));

        log.info("------> Все ЗАПАСЫ из приходников в 1с найдены и сохранены в базу");

        Page<InvoiceStocksEntity> result = invoiceStocksRepository.findAll(PageRequest.of(0, 10));
        log.info("📄 Возвращаем первые {} записей из {} всего", result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    @Override
    public void getInvoiceStocksById(UUID uuid) {
        log.info("🔍 Поиск запасов по одному ID: {}", uuid);

        long startTime = System.currentTimeMillis();

        try {
            String url = String.format("/Document_ПриходнаяНакладная_Запасы?" +
                    "$filter=Ref_Key eq guid'%s'&" +
                    "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                    "$format=json", uuid);

            log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

            InvoiceStocksResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceStocksResponseDto.class);

            assert response != null;
            List<InvoiceStocksItemResponseDto> items = response.getValue();

            if (!items.isEmpty()) {
                long saveStartTime = System.currentTimeMillis();

                for (InvoiceStocksItemResponseDto value : items) {
                    invoiceStocksRepository.save(invoiceStocksMapper.toEntity(value));
                }

                long saveTime = System.currentTimeMillis() - saveStartTime;
                long totalTime = System.currentTimeMillis() - startTime;

                log.info("✅ ID {}: найдено {} записей, сохранено в БД (время сохранения: {} мс, общее время: {} мс)",
                        uuid, items.size(), saveTime, totalTime);
            } else {
                log.info("⚠️ ID {}: записи не найдены (время поиска: {} мс)",
                        uuid, System.currentTimeMillis() - startTime);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при получении ЗАПАСОВ из приходных накладных по id {}: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    @Override
    public List<InvoiceStocksEntity> findInvoiceStocksByIds(Set<UUID> ids) {
        log.info("===== ПОИСК ЗАПАСОВ ПО СПИСКУ ID ПРИХОДНЫХ НАКЛАДНЫХ =====");

        if (ids == null || ids.isEmpty()) {
            log.warn("⚠️ Список ID пуст или равен null");
            return Collections.emptyList();
        }

        log.info("Всего ID для поиска: {}", ids.size());
        log.info("Первые 10 ID: {}", ids.stream().limit(10).collect(Collectors.toList()));

        List<InvoiceStocksEntity> allEntitiesToSave = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger foundCount = new AtomicInteger(0);
        AtomicLong totalItemsFound = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        // Создаем пул потоков
        int threadPoolSize = Math.min(MAX_CONCURRENT_REQUESTS, ids.size());
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Создан пул потоков на {} потоков для обработки {} ID", threadPoolSize, ids.size());

        try {
            // Создаем список задач
            List<CompletableFuture<Void>> futures = ids.stream()
                    .map(id -> CompletableFuture.runAsync(() -> {
                        long requestStartTime = System.currentTimeMillis();
                        String threadName = Thread.currentThread().getName();
                        int currentProcessed = processedCount.incrementAndGet();

                        log.debug("[Поток: {}] Обработка ID {}/{}: {}",
                                threadName, currentProcessed, ids.size(), id);

                        try {
                            List<InvoiceStocksEntity> entities = fetchStocksForId(id);

                            if (!entities.isEmpty()) {
                                allEntitiesToSave.addAll(entities);
                                int found = foundCount.incrementAndGet();
                                long itemsFound = totalItemsFound.addAndGet(entities.size());

                                long requestTime = System.currentTimeMillis() - requestStartTime;
                                log.info("[Поток: {}] ✅ ID {}: найдено {} записей (всего найдено ID: {}, всего записей: {}, время: {} мс)",
                                        threadName, id, entities.size(), found, itemsFound, requestTime);
                            } else {
                                log.debug("[Поток: {}] ⚠️ ID {}: записи не найдены", threadName, id);
                            }
                        } catch (Exception e) {
                            log.error("[Поток: {}] ❌ Ошибка при обработке ID {}: {}", threadName, id, e.getMessage(), e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Ждем выполнения всех задач с таймаутом
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);

            log.info("✅ Все запросы завершены");

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("❌ Ошибка или таймаут при выполнении запросов: {}", e.getMessage(), e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("⚠️ Принудительное завершение потоков");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("❌ Ошибка при завершении пула потоков: {}", e.getMessage());
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // Сохранение результатов
        if (!allEntitiesToSave.isEmpty()) {
            log.info("💾 Начинаем сохранение {} записей в БД...", allEntitiesToSave.size());
            long saveStartTime = System.currentTimeMillis();

            try {
                List<InvoiceStocksEntity> savedEntities = invoiceStocksRepository.saveAll(allEntitiesToSave);
                long saveTime = System.currentTimeMillis() - saveStartTime;
                log.info("✅ Успешно сохранено {} записей в БД за {} мс", savedEntities.size(), saveTime);
            } catch (Exception e) {
                log.error("❌ Ошибка при сохранении запасов в БД: {}", e.getMessage(), e);
                saveEntitiesIndividually(allEntitiesToSave);
            }
        } else {
            log.info("⚠️ Нет записей для сохранения в БД");
        }

        log.info("===== СТАТИСТИКА ПОИСКА =====");
        log.info("📊 Всего обработано ID: {}", ids.size());
        log.info("✅ Найдено ID с запасами: {} ({}% от общего)",
                foundCount.get(),
                String.format("%.1f", (foundCount.get() * 100.0 / ids.size())));
        log.info("📦 Всего найдено записей запасов: {}", totalItemsFound.get());
        log.info("⏱️ Общее время выполнения: {} мс ({} сек)", totalTime, totalTime / 1000);
        log.info("📈 Средняя скорость: {} ID/сек",
                ids.size() / (totalTime / 1000 > 0 ? totalTime / 1000 : 1));

        return allEntitiesToSave;
    }

    private List<InvoiceStocksEntity> fetchStocksForId(UUID id) {
        String url = String.format(
                "/Document_ПриходнаяНакладная_Запасы?" +
                        "$filter=Ref_Key eq guid'%s'&" +
                        "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                        "$format=json", id);

        try {
            InvoiceStocksResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceStocksResponseDto.class);

            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                return response.getValue().stream()
                        .map(invoiceStocksMapper::toEntity)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении ЗАПАСОВ приходника для id: {}", id, e);
            throw new RuntimeException("Ошибка получения данных из 1С для id: " + id, e);
        }

        return Collections.emptyList();
    }

    /**
     * Резервный метод для сохранения записей по одной в случае ошибки пакетного сохранения
     */
    private void saveEntitiesIndividually(List<InvoiceStocksEntity> entities) {
        log.info("🔄 Пробуем сохранить записи по одной (всего: {})", entities.size());

        int successCount = 0;
        int errorCount = 0;
        long totalStartTime = System.currentTimeMillis();

        for (InvoiceStocksEntity entity : entities) {
            try {
                long saveStartTime = System.currentTimeMillis();
                invoiceStocksRepository.save(entity);
                successCount++;
                log.debug("✅ Сохранена запись {}/{} (ID: {}, время: {} мс)",
                        successCount + errorCount, entities.size(), entity.getRefKey(),
                        System.currentTimeMillis() - saveStartTime);
            } catch (Exception e) {
                errorCount++;
                log.error("❌ Ошибка при сохранении записи: {} (Ref_Key: {})",
                        e.getMessage(), entity.getRefKey());
            }
        }

        long totalTime = System.currentTimeMillis() - totalStartTime;
        log.info("📊 Итоги индивидуального сохранения: успешно {}, ошибок {}, общее время {} мс",
                successCount, errorCount, totalTime);
    }

    private InvoiceStocksResponseDto getInvoiceStocks(Integer top, Integer skip) {
        String url = String.format("/Document_ПриходнаяНакладная_Запасы?" +
                "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        log.debug("URL запроса: {}", url.replaceAll("['\"]", ""));

        try {
            InvoiceStocksResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InvoiceStocksResponseDto.class);

            return response;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении ЗАПАСОВ из приходных накладных с skip={}: {}", skip, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }
}