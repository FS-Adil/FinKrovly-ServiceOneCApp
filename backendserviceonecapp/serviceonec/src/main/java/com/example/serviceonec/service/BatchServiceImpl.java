package com.example.serviceonec.service;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.BatchItemResponseDto;
import com.example.serviceonec.model.dto.response.BatchResponseDto;
import com.example.serviceonec.model.dto.response.NomenclatureResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryResponseDto;
import com.example.serviceonec.model.entity.BatchEntity;
import com.example.serviceonec.model.mapper.BatchMapper;
import com.example.serviceonec.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final RestClientConfig restClientConfig;

    private final BatchRepository batchRepository;

    private final BatchMapper batchMapper;

    @Override
    public Page<BatchEntity> getAllBatch() {
        log.info("-->BatchServiceImpl-->getAllBatch--вызов метода");

        batchRepository.deleteAll();
        log.info("-->Чистим таблицу batches");

        boolean isStop = true;
        int top = 100;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            BatchResponseDto batchResponseDto = getResponse(
                    top,
                    skip
            );

            if (batchResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            try {
                for (BatchItemResponseDto value
                        : batchResponseDto.getValue()) {
                    batchRepository.save(batchMapper.toEntity(value));
                }
            } catch (DataIntegrityViolationException e) {
                log.error("Ошибка целостности данных: {}", e.getMessage());
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все Партии из 1с найдены и сохранены в базу");

        return batchRepository.findAll(PageRequest.of(0, 10));
    }

    private BatchResponseDto getResponse(
            Integer top,
            Integer skip
    ) {
        log.info("------> Старт метода по поиску в 1с всех Партий");
        String url = String.format("/Catalog_ПартииНоменклатуры?" +
                "$select=Ref_Key,Description,Code,_Цена,_Вес,_Количество,_Длина,_ПартияДата&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        BatchResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(BatchResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Партий за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех Партий");

        return response;
    }
}
