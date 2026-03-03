package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.example.serviceonec.model.dto.response.NomenclatureResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.mapper.NomenclatureMapper;
import com.example.serviceonec.repository.NomenclatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class NomenclatureServiceImpl implements NomenclatureService {

    private final RestClientConfig restClientConfig;
    private final NomenclatureMapper nomenclatureMapper;
    private final NomenclatureRepository nomenclatureRepository;

    private static final int PAGE_SIZE = 500; // Размер страницы
    private static final int THREAD_POOL_SIZE = 10; // Количество потоков
    private static final int TIMEOUT_MINUTES = 20; // Таймаут

    @Override
    public Page<NomenclatureEntity> getAllNomenclature() {
        log.info("-------> NomenclatureServiceImpl --------> getAllNomenclature");

        // Очищаем таблицу
        nomenclatureRepository.deleteAll();
        log.info("Таблица номенклатуры очищена");

        // Конкурентная очередь для страниц, которые нужно обработать
        Queue<Integer> pagesToProcess = new ConcurrentLinkedQueue<>(); // Этот момент все еще не идеален, но для простоты оставим

        // ИСПОЛЬЗУЕМ CONCURRENT HASH MAP ДЛЯ ДЕДУПЛИКАЦИИ ПО КОДУ
        Map<String, NomenclatureEntity> entityMap = new ConcurrentHashMap<>();

        AtomicInteger currentPage = new AtomicInteger(0);
        AtomicBoolean hasMorePages = new AtomicBoolean(true);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    while (hasMorePages.get()) {
                        try {
                            int pageNumber = currentPage.getAndIncrement();
                            int skip = pageNumber * PAGE_SIZE;

                            log.debug("Поток {} загружает страницу {} (skip: {})",
                                    Thread.currentThread().getName(), pageNumber, skip);

                            NomenclatureResponseDto response = getNomenclature(PAGE_SIZE, skip);

                            if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
                                log.info("Получена пустая страница на pageNumber: {}, завершаем загрузку", pageNumber);
                                hasMorePages.set(false);
                                break;
                            }

                            // Преобразуем каждую запись в сущность и кладем в Map
                            response.getValue().forEach(dto -> {
                                NomenclatureEntity entity = nomenclatureMapper.toEntity(dto);
                                // put если код не null, иначе можно пропустить или сгенерировать исключение
                                if (entity.getCode() != null) {
                                    entityMap.put(entity.getCode(), entity);
                                } else {
                                    log.warn("Получена запись с null кодом, пропускаем: {}", dto);
                                }
                            });

                            log.info("Страница {} загружена, уникальных записей в мапе: {}", pageNumber, entityMap.size());

                        } catch (Exception e) {
                            log.error("Ошибка при загрузке страницы", e);
                        }
                    }
                }, executor);
                futures.add(future);
            }

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            try {
                allFutures.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.error("Превышен таймаут выполнения загрузки ({} минут)", TIMEOUT_MINUTES);
                hasMorePages.set(false);
                futures.forEach(f -> f.cancel(true));
            }

            List<NomenclatureEntity> uniqueEntities = new ArrayList<>(entityMap.values());
            log.info("Всего загружено УНИКАЛЬНЫХ записей номенклатуры: {}", uniqueEntities.size());

            // Сохраняем в базу данных
            saveEntitiesInBatches(uniqueEntities);

        } catch (Exception e) {
            log.error("Ошибка при загрузке номенклатуры", e);
            throw new RuntimeException("Ошибка при загрузке номенклатуры", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return nomenclatureRepository.findAll(PageRequest.of(0, 10));
    }

    private NomenclatureResponseDto getNomenclature(Integer top, Integer skip) {
        String url = String.format("/Catalog_Номенклатура?" +
                "$select=Description,Code,Ref_Key&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Code desc&" +
                "$format=json", top, skip);

        try {
            return restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(NomenclatureResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка при получении Номенклатур (top: {}, skip: {})", top, skip, e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }
    }

    private void saveEntitiesInBatches(List<NomenclatureEntity> entities) {
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) entities.size() / batchSize);

        log.info("Сохранение {} записей в базу данных батчами по {}", entities.size(), batchSize);

        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<NomenclatureEntity> batch = entities.subList(i, end);

            try {
                nomenclatureRepository.saveAll(batch);
                log.info("Сохранен батч {}/{} (записей: {})",
                        (i / batchSize) + 1, totalBatches, batch.size());
            } catch (Exception e) {
                log.error("Ошибка при сохранении батча {}/{}", (i / batchSize) + 1, totalBatches, e);
                // Пробуем сохранить по одной записи в случае ошибки
                saveEntitiesIndividually(batch);
            }
        }
    }

    private void saveEntitiesIndividually(List<NomenclatureEntity> entities) {
        log.info("Пытаемся сохранить {} записей по одной", entities.size());
        int successCount = 0;

        for (NomenclatureEntity entity : entities) {
            try {
                nomenclatureRepository.save(entity);
                successCount++;
            } catch (Exception e) {
                log.error("Не удалось сохранить номенклатуру с кодом: {}", entity.getCode(), e);
            }
        }

        log.info("Успешно сохранено {} из {} записей", successCount, entities.size());
    }
}