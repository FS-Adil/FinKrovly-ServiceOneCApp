package com.example.serviceonec.service.production;

import com.example.serviceonec.config.RestClientConfig;
import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryResponseDto;
import com.example.serviceonec.model.dto.response.production.ProductionItemResponseDto;
import com.example.serviceonec.model.dto.response.production.ProductionResponseDto;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionItemsEntity;
import com.example.serviceonec.model.entity.production.ProductionStocksEntity;
import com.example.serviceonec.model.mapper.production.ProductionItemsMapper;
import com.example.serviceonec.model.mapper.production.ProductionMapper;
import com.example.serviceonec.model.mapper.production.ProductionStocksMapper;
import com.example.serviceonec.repository.production.ProductionItemsRepository;
import com.example.serviceonec.repository.production.ProductionRepository;
import com.example.serviceonec.repository.production.ProductionStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionServiceImpl implements ProductionService{

    private final RestClientConfig restClientConfig;

    private final ProductionRepository productionRepository;
    private final ProductionStocksRepository productionStocksRepository;
    private final ProductionItemsRepository productionItemsRepository;

    private final ProductionMapper productionMapper;
    private final ProductionStocksMapper productionStocksMapper;
    private final ProductionItemsMapper productionItemsMapper;

    @Override
    public Page<ProductionEntity> getAllProduction(LocalDateTime startDate, LocalDateTime endDate) {

        log.info("-------> ProductionServiceImpl -------> getAllProduction");

        productionRepository.deleteAll();
        log.info("------> Очистка таблицы productions");
        productionStocksRepository.deleteAll();
        log.info("------> Очистка таблицы production_stocks");
        productionItemsRepository.deleteAll();
        log.info("------> Очистка таблицы production_items");

        boolean isStop = true;
        int top = 500;
        int skip = 0;

        while (isStop) {

//            log.info("------> Цикл с данными запроса: top({}) - skip({})", top, skip);

            ProductionResponseDto productionResponseDto = getResponse(
                    top,
                    skip,
                    startDate,
                    endDate
            );

            if (productionResponseDto.getValue().isEmpty()) {
                isStop = false;
            }

                for (ProductionItemResponseDto value
                        : productionResponseDto.getValue()) {

                    try {
                        UUID refKey = value.getRefKey();
                        productionRepository.save(productionMapper.toEntity(value));
//                        log.info("------> Документ производство ref_key({}) из top({}) - skip({})  найдены и сохранены в базу", refKey, top, skip);

                        for (ProductionItemResponseDto.ProductionStocksDto stock
                                : value.getStocks()) {
                            productionStocksRepository.save(productionStocksMapper.toEntity(stock));
                        }
//                        log.info("------> Документ производство ref_key({}) ЗАПАСЫ из top({}) - skip({})  найдены и сохранены в базу", refKey, top, skip);

                        for (ProductionItemResponseDto.ProductionItemsDto item
                                : value.getProducts()) {
                            productionItemsRepository.save(productionItemsMapper.toEntity(item));
                        }
//                        log.info("------> Документ производство ref_key({}) ПРОДУКЦИЯ из top({}) - skip({})  найдены и сохранены в базу", refKey, top, skip);

                    } catch (DataIntegrityViolationException e) {
                        log.error("Ошибка целостности данных: {}", e.getMessage());
                    }
                }

//            log.info("------> Производства из top({}) - skip({})  найдены и сохранены в базу", top, skip);

            skip+=top;

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("------> Все Производства из 1с найдены и сохранены в базу");

        return productionRepository.findAll(PageRequest.of(0, 10));
    }

    @Override
    public Page<ProductionItemsEntity> getAllProductionItems() {
        return null;
    }

    @Override
    public Page<ProductionStocksEntity> getAllProductionStocks() {
        return null;
    }

    private ProductionResponseDto getResponse(
            Integer top,
            Integer skip,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

//        log.info("------> Старт метода по поиску в 1с всех Производств");

        String url = String.format("/Document_СборкаЗапасов?" +
                "$filter=Posted eq true" +
//                " and ВидОперации eq 'ПоступлениеОтПоставщика'" +
                " and Date ge datetime'" + startDate + "'" +
                " and Date le datetime'" + endDate + "'" +
                "&" +
                "$select=Ref_Key, Number, Date, ЗаказПокупателя_Key, Организация_Key, Продукция, Запасы&" +
                "$orderby=Date desc&" +
                "$top=%s&$skip=%s&" +
                "$format=json", top, skip);

        ProductionResponseDto response;

        try {

            response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(ProductionResponseDto.class);

        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении Производств за период с %s по %s", 1, 2), String.valueOf(e)
            );
            throw new RuntimeException("Ошибка получения данных из 1С", e);
        }

//        log.info("------> Конец метода по поиску в 1с всех Производств");

        return response;
    }
}
