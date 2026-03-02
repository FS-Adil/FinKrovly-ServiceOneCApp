package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.BatchItemResponseDto;
import com.example.serviceonec.model.dto.response.BatchResponseDto;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.mapper.BatchMapper;
import com.example.serviceonec.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final RestClientConfig restClientConfig;
    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;

    private static final int PAGE_SIZE = 500; // Размер страницы
    private static final int THREAD_POOL_SIZE = 5; // Количество потоков
    private static final int TIMEOUT_MINUTES = 20; // Таймаут

    @Override
    public Page<BatchEntity> getAllBatch() {
        log.info("-->BatchServiceImpl-->getAllBatch--вызов метода");

        // Очищаем таблицу
        batchRepository.deleteAll();
        log.info("-->Чистим таблицу batches");

        // Очередь для сбора всех сущностей перед сохранением
        List<BatchEntity> allEntities = Collections.synchronizedList(new ArrayList<>());

        AtomicInteger currentPage = new AtomicInteger(0);
        AtomicBoolean hasMorePages = new AtomicBoolean(true);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Запускаем пул потоков для загрузки данных
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    while (hasMorePages.get()) {
                        try {
                            int pageNumber = currentPage.getAndIncrement();
                            int skip = pageNumber * PAGE_SIZE;

                            log.debug("Поток {} загружает страницу {} (skip: {})",
                                    Thread.currentThread().getName(), pageNumber, skip);

                            BatchResponseDto response = getResponse(PAGE_SIZE, skip);

                            // Проверяем на пустой ответ
                            if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
                                log.info("Получена пустая страница на pageNumber: {}, завершаем загрузку", pageNumber);
                                hasMorePages.set(false);
                                break;
                            }

                            // Преобразуем каждую запись в сущность и добавляем в общий список
                            List<BatchEntity> pageEntities = new ArrayList<>();

                            for (BatchItemResponseDto dto : response.getValue()) {
                                try {
                                    BatchEntity entity = batchMapper.toEntity(dto);
                                    pageEntities.add(entity);
                                } catch (Exception e) {
                                    log.error("Ошибка при маппинге записи: {}", e.getMessage());
                                }
                            }

                            // Добавляем все сущности страницы в общий список
                            allEntities.addAll(pageEntities);

                            log.info("Страница {} загружена, всего записей в списке: {}",
                                    pageNumber, allEntities.size());

                            // Небольшая задержка для снижения нагрузки на 1С
                            try {
                                TimeUnit.MILLISECONDS.sleep(20);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("Поток прерван во время задержки");
                                break;
                            }

                        } catch (Exception e) {
                            log.error("Ошибка при загрузке страницы", e);

                            // В случае ошибки продолжаем загрузку со следующей страницей
                            // Не устанавливаем hasMorePages в false, чтобы не прерывать другие потоки
                        }
                    }
                }, executor);
                futures.add(future);
            }

            // Ожидаем завершения всех задач с таймаутом
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            try {
                allFutures.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
                log.info("Все потоки завершили загрузку данных");
            } catch (TimeoutException e) {
                log.error("Превышен таймаут выполнения загрузки ({} минут)", TIMEOUT_MINUTES);
                hasMorePages.set(false);
                futures.forEach(f -> f.cancel(true));
            } catch (InterruptedException e) {
                log.error("Загрузка была прервана", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("Ошибка при выполнении загрузки", e);
            }

            log.info("------> Всего загружено записей партий: {}", allEntities.size());

            // Сохраняем в базу данных батчами
            if (!allEntities.isEmpty()) {
                saveEntitiesInBatches(allEntities);
            } else {
                log.warn("Нет данных для сохранения");
            }

        } catch (Exception e) {
            log.error("Критическая ошибка при загрузке партий", e);
            throw new RuntimeException("Ошибка при загрузке партий", e);
        } finally {
            // Корректное завершение ExecutorService
            shutdownExecutor(executor);
        }

        log.info("------> Все Партии из 1с найдены и сохранены в базу");
        return batchRepository.findAll(PageRequest.of(0, 10));
    }

    private BatchResponseDto getResponse(Integer top, Integer skip) {
        String url = String.format("/Catalog_ПартииНоменклатуры?" +
                "$select=Ref_Key,Description,Code,Цена,Вес,Количество,Длина,ДатаПартии&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Code desc&" +
                "$format=json", top, skip);

        try {
            return restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(BatchResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка при получении Партий (top: {}, skip: {})", top, skip, e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    private void saveEntitiesInBatches(List<BatchEntity> entities) {
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) entities.size() / batchSize);

        log.info("Сохранение {} записей в базу данных батчами по {}", entities.size(), batchSize);

        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<BatchEntity> batch = entities.subList(i, end);

            try {
                batchRepository.saveAll(batch);
                log.info("Сохранен батч {}/{} (записей: {})",
                        (i / batchSize) + 1, totalBatches, batch.size());
            } catch (DataIntegrityViolationException e) {
                log.error("Ошибка целостности данных при сохранении батча {}/{}: {}",
                        (i / batchSize) + 1, totalBatches, e.getMessage());
                // Пробуем сохранить по одной записи в случае ошибки
                saveEntitiesIndividually(batch);
            } catch (Exception e) {
                log.error("Ошибка при сохранении батча {}/{}", (i / batchSize) + 1, totalBatches, e);
                // Пробуем сохранить по одной записи в случае ошибки
                saveEntitiesIndividually(batch);
            }
        }
    }

    private void saveEntitiesIndividually(List<BatchEntity> entities) {
        log.info("Пытаемся сохранить {} записей по одной", entities.size());
        int successCount = 0;

        for (BatchEntity entity : entities) {
            try {
                batchRepository.save(entity);
                successCount++;
            } catch (DataIntegrityViolationException e) {
                log.error("Не удалось сохранить партию с Ref_Key: {} - ошибка целостности данных",
                        entity.getRefKey(), e);
            } catch (Exception e) {
                log.error("Не удалось сохранить партию с Ref_Key: {}", entity.getRefKey(), e);
            }
        }

        log.info("Успешно сохранено {} из {} записей", successCount, entities.size());
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Executor не завершился");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}