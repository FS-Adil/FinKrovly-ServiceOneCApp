package com.example.serviceonec.service.inventory;

import com.example.serviceonec.config.RestClientConfig;

import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryResponseDto;
import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import com.example.serviceonec.model.entity.inventory.InventoryStocksEntity;
import com.example.serviceonec.model.mapper.inventory.InventoryMapper;
import com.example.serviceonec.model.mapper.inventory.InventoryStocksMapper;
import com.example.serviceonec.repository.inventory.InventoryRepository;
import com.example.serviceonec.repository.inventory.InventoryStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final RestClientConfig restClientConfig;

    private final InventoryRepository inventoryRepository;
    private final InventoryStocksRepository inventoryStocksRepository;

    private final InventoryMapper inventoryMapper;
    private final InventoryStocksMapper inventoryStocksMapper;

    @Override
    public Page<InventoryEntity> getAllInventory() {

        inventoryRepository.deleteAll();
        inventoryStocksRepository.deleteAll();

        boolean isStop = true;
        int top = 100;
        int skip = 0;

        while (isStop) {

            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            InventoryResponseDto inventoryResponseDto = getResponse(
                    top,
                    skip
            );

            if (inventoryResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

            for (InventoryItemResponseDto value
                    : inventoryResponseDto.getValue()) {
                inventoryRepository.save(inventoryMapper.toEntity(value));

                for (InventoryItemResponseDto.InventoryStocksResponseDto stock
                        : value.getStocks()) {
                    inventoryStocksRepository.save(inventoryStocksMapper.toEntity(stock));
                }
            }

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все Оприходования из 1с найдены и сохранены в базу");

        return inventoryRepository.findAll(PageRequest.of(0, 10));
    }

    @Override
    public Page<InventoryStocksEntity> getAllInventoryStocks() {
        inventoryRepository.deleteAll();

        return inventoryStocksRepository.findAll(PageRequest.of(0,10));
    }

    private InventoryResponseDto getResponse(
            Integer top,
            Integer skip
    ) {

        log.info("------> Старт метода по поиску в 1с всех Оприходований");

        String url = String.format("/Document_ОприходованиеЗапасов?" +
                "$filter=Posted eq true" +
//                " and ВидОперации eq 'ПоступлениеОтПоставщика'" +
//                " and Date ge datetime'" + startDate + "'" +
//                " and Date le datetime'" + endDate + "'" +
                "&" +
                "$select=Ref_Key,Number,Date,Организация_Key,СтруктурнаяЕдиница_Key,Запасы&" +
                "$top=%s&$skip=%s&" +
                "$orderby=Date desc&" +
                "$format=json", top, skip);

        InventoryResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(InventoryResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Оприходований за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

        log.info("------> Конец метода по поиску в 1с всех Оприходований");

        return response;
    }
}
