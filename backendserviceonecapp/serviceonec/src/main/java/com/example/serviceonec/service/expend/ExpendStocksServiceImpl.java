package com.example.serviceonec.service.expend;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.expend.ExpendStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.expend.ExpendStocksResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksItemResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.mapper.expend.ExpendStocksMapper;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpendStocksServiceImpl implements ExpendStocksService {

    private final RestClientConfig restClientConfig;
    private final ExpendStocksRepository expendStocksRepository;
    private final ExpendStocksMapper expendStocksMapper;

    @Override
    public Page<ExpendStocksEntity> getAllExpendStocks() {

        log.info("------> ExpendStocksServiceImpl ------> getAllExpendStocks");

        expendStocksRepository.deleteAll();

        boolean isStop = true;
        int top = 500;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            ExpendStocksResponseDto expendStocksResponseDto = getExpendStocks(top, skip);

            if (expendStocksResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            for (ExpendStocksItemResponseDto value : expendStocksResponseDto.getValue()) {
                expendStocksRepository.save(expendStocksMapper.toEntity(value));
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все ЗАПАСЫ из расходников в 1с найдены и сохранены в базу");

        return expendStocksRepository.findAll(PageRequest.of(0, 10));
    }

    @Override
    public Map<UUID, List<ExpendStocksEntity>> findExpendStocksByIds(List<UUID> ids) {
        log.info("------> Старт метода по поиску в 1с ЗАПАСОВ из расходников по списку id");

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, List<ExpendStocksEntity>> resultMap = new ConcurrentHashMap<>();
        List<ExpendStocksEntity> allEntitiesToSave = Collections.synchronizedList(new ArrayList<>());

        // Создаем пул потоков для параллельных запросов
        ExecutorService executor = Executors.newFixedThreadPool(5); // 5 параллельных запросов

        List<CompletableFuture<Void>> futures = ids.stream()
                .map(id -> CompletableFuture.runAsync(() ->
                        fetchAndProcessId(id, resultMap, allEntitiesToSave), executor))
                .collect(Collectors.toList());

        // Ждем завершения всех запросов
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();

        // Пакетное сохранение всех найденных записей
        if (!allEntitiesToSave.isEmpty()) {
            try {
                expendStocksRepository.saveAll(allEntitiesToSave);
                log.info("Сохранено {} записей запасов в БД", allEntitiesToSave.size());
            } catch (Exception e) {
                log.error("Ошибка при пакетном сохранении запасов в БД", e);
                saveEntitiesIndividually(allEntitiesToSave);
            }
        }

        log.info("Найдено запасов для {} расходников из {} запрошенных",
                resultMap.size(), ids.size());
        log.info("------> Конец метода по поиску в 1с всех ЗАПАСОВ из расходников по списку id");

        return resultMap;
    }

    private void fetchAndProcessId(UUID id, Map<UUID, List<ExpendStocksEntity>> resultMap,
                                   List<ExpendStocksEntity> allEntitiesToSave) {
        try {
            String url = String.format(
                    "/Document_РасходнаяНакладная_Запасы?" +
                            "$filter=Ref_Key eq guid'%s'&" +
                            "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                            "$format=json", id);

            ExpendStocksResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendStocksResponseDto.class);

            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                List<ExpendStocksEntity> entities = response.getValue().stream()
                        .map(expendStocksMapper::toEntity)
                        .collect(Collectors.toList());

                allEntitiesToSave.addAll(entities);
                resultMap.put(id, entities);
            } else {
                log.warn("По id {} запасов из Расходников нет", id);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении ЗАПАСОВ для id: {}", id, e);
        }
    }

    /**
     * Резервный метод для сохранения записей по одной в случае ошибки пакетного сохранения
     */
    private void saveEntitiesIndividually(List<ExpendStocksEntity> entities) {
        log.info("Пробуем сохранить записи по одной");
        int successCount = 0;
        for (ExpendStocksEntity entity : entities) {
            try {
                expendStocksRepository.save(entity);
                successCount++;
            } catch (Exception e) {
                log.error("Ошибка при сохранении записи: {}", entity, e);
            }
        }
        log.info("Сохранено {} из {} записей по одной", successCount, entities.size());
    }

    @Override
    public void findExpendStocksById(UUID id) {
//        log.info("------> Старт метода по поиску в 1с ЗАПАСОВ из расходника по id Расходника");

        String url = String.format(
                "/Document_РасходнаяНакладная_Запасы?" +
                "$filter=Ref_Key eq guid'%s'&" +
                "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                "$format=json", id);

        ExpendStocksResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendStocksResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении ЗАПАСОВ из расходных накладных за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        assert response != null;

        if (response.getValue().isEmpty()) {
            log.warn("По данному id {} запасов из Расходников нет", id);
        } else {
            for (ExpendStocksItemResponseDto value : response.getValue()) {
                expendStocksRepository.save(expendStocksMapper.toEntity(value));
            }
//            log.info("По данному id {} все запасы найдены и сохранены в БД", id);
        }

//        log.info("------> Конец метода по поиску в 1с всех ЗАПАСОВ из расходников по id Расходника");
    }

    private ExpendStocksResponseDto getExpendStocks(Integer top, Integer skip) {
//        log.info("------> Старт метода по поиску в 1с всех ЗАПАСОВ из расходника");

        String url = String.format("/Document_РасходнаяНакладная_Запасы?" +
//                "$filter=Date ge datetime'" + startStr + "' " +
//                    "and Date le datetime'" + endStr + "'" +
//                "&" +
                "$select=Ref_Key,LineNumber,Номенклатура_Key,Характеристика_Key,Партия_Key,Количество,ЕдиницаИзмерения,Цена&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        ExpendStocksResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ExpendStocksResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении ЗАПАСОВ из расходных накладных за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

//        log.info("------> Конец метода по поиску в 1с всех ЗАПАСОВ из расходников");
        return response;
    }
}
