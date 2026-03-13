package com.example.serviceonec.service.specification;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.specification.SpecificationResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.specification.SpecificationEntity;
import com.example.serviceonec.model.mapper.specification.SpecificationMapper;
import com.example.serviceonec.repository.specification.SpecificationRepository;
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
public class SpecificationServiceImpl implements SpecificationService{

    private final RestClientConfig restClientConfig;

    private final SpecificationRepository specificationRepository;

    private final SpecificationMapper specificationMapper;

    private static final int PAGE_SIZE = 500; // Размер страницы
    private static final int THREAD_POOL_SIZE = 5; // Количество потоков
    private static final int TIMEOUT_MINUTES = 20; // Таймаут

    @Override
    public Page<SpecificationEntity> getAllSpecification() {
        log.info("-------> SpecificationServiceImpl --------> getAllSpecification");

        // Очищаем таблицу
        specificationRepository.deleteAll();
        log.info("Таблица specification очищена");

        // Конкурентная очередь для страниц, которые нужно обработать
        Queue<Integer> pagesToProcess = new ConcurrentLinkedQueue<>(); // Этот момент все еще не идеален, но для простоты оставим

        // ИСПОЛЬЗУЕМ CONCURRENT HASH MAP ДЛЯ ДЕДУПЛИКАЦИИ ПО КОДУ
        Map<UUID, SpecificationEntity> entityMap = new ConcurrentHashMap<>();

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

                            SpecificationResponseDto response = getResponse(PAGE_SIZE, skip);

                            if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
                                log.info("Получена пустая страница на pageNumber: {}, завершаем загрузку", pageNumber);
                                hasMorePages.set(false);
                                break;
                            }

                            // Преобразуем каждую запись в сущность и кладем в Map
                            response.getValue().forEach(dto -> {
                                SpecificationEntity entity = specificationMapper.toEntity(dto);
                                // put если код не null, иначе можно пропустить или сгенерировать исключение
                                if (entity.getRefKey() != null) {
                                    entityMap.put(entity.getRefKey(), entity);
                                } else {
                                    log.warn("Получена запись с null кодом, пропускаем: {}", dto);
                                }
                            });

                            log.debug("Страница {} загружена, уникальных записей в мапе: {}", pageNumber, entityMap.size());

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

            List<SpecificationEntity> uniqueEntities = new ArrayList<>(entityMap.values());
            log.info("Всего загружено УНИКАЛЬНЫХ записей спецификации: {}", uniqueEntities.size());

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
        return specificationRepository.findAll(PageRequest.of(0, 10));
    }

    private SpecificationResponseDto getResponse(Integer top, Integer skip) {
        String url = String.format("/Catalog_Спецификации_Состав?" +
                "$select=Ref_Key, Количество&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        try {
            return restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(SpecificationResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка при получении Спецификации (top: {}, skip: {})", top, skip, e);
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }


    }

    private void saveEntitiesInBatches(List<SpecificationEntity> entities) {
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) entities.size() / batchSize);

        log.info("Сохранение {} записей в базу данных батчами по {}", entities.size(), batchSize);

        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<SpecificationEntity> batch = entities.subList(i, end);

            try {
                specificationRepository.saveAll(batch);
                log.info("Сохранен батч {}/{} (записей: {})",
                        (i / batchSize) + 1, totalBatches, batch.size());
            } catch (Exception e) {
                log.error("Ошибка при сохранении батча {}/{}", (i / batchSize) + 1, totalBatches, e);
                // Пробуем сохранить по одной записи в случае ошибки
                saveEntitiesIndividually(batch);
            }
        }
    }

    private void saveEntitiesIndividually(List<SpecificationEntity> entities) {
        log.info("Пытаемся сохранить {} записей по одной", entities.size());
        int successCount = 0;

        for (SpecificationEntity entity : entities) {
            try {
                specificationRepository.save(entity);
                successCount++;
            } catch (Exception e) {
                log.error("Не удалось сохранить номенклатуру с кодом: {}", entity.getRefKey(), e);
            }
        }

        log.info("Успешно сохранено {} из {} записей", successCount, entities.size());
    }
}
