package com.example.serviceonec.Monitoring.service.production;

import com.example.serviceonec.Monitoring.dto.input.MonitoringProductionInputResponseDto.*;
import com.example.serviceonec.Monitoring.dto.output.MonitoringProductionOutputResponseDto;
import com.example.serviceonec.Monitoring.dto.output.MonitoringProductionOutputResponseDto.*;
import com.example.serviceonec.config.RestClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringProductionDetailsBuildData {

    private final RestClientConfig restClientConfig;

    private final List<ProductionDetail> resultListOne = new ArrayList<>();

    public List<ProductionDetail> getDetailsProductionOne() {

        resultListOne.clear();

        List<String> whiteList = List.of(
                "4ebabf76-5c8d-11e2-886b-f46d0466b92f",
                "37bd2f65-a311-11e5-89a5-f46d0466b92f",
                "4ebabe41-5c8d-11e2-886b-f46d0466b92f",
                "0c8d22ef-8cb8-11f0-8276-000c29f4122d",
                "31596db3-8cb8-11f0-8276-000c29f4122d",
                "d4aee547-3618-11eb-810e-000c29aa9162",
                "220059fd-2297-11f0-825d-000c29f4122d",
                "54cd8d1a-5c8d-11e2-886b-f46d0466b92f",
                "a05ee4f2-d8b3-11eb-812c-000c29aa9162",
                "3e0c360b-8cb8-11f0-8276-000c29f4122d",
                "6191aba3-805d-11f1-8281-000c29f4122d",
                "03f55e48-019d-11f1-827c-000c29f4122d"
        );

        Map<UUID, ReceiptProduction> receiptProductionMap = new HashMap<>();
        Map<UUID, List<ProductionItemsDto>> productionItemsDtoMap = new HashMap<>();
        Map<UUID, List<ProductionDistributionStocksDto>> productionDistributionStocksDtoMap = new HashMap<>();

        int skip = 0;
        int top = 500;
        boolean hasMoreData = true;

        while (hasMoreData) {
            // получаем из 1с документы на производство где нет связи с заказом покупателя
            MonitoringProductionOutputResponseDto responseDto = getResponseProduction(top, skip);

            if (responseDto == null || responseDto.getValue().isEmpty()) {
                hasMoreData = false;
                if (skip == 0) {
                    log.info("--> Нет документов на производство!");
                    return List.of();
                }
                break;
            }

            for (ReceiptProduction receiptProduction : responseDto.getValue()) {
                UUID refKey = receiptProduction.getRefKey();

                receiptProductionMap.put(refKey, receiptProduction);
                productionItemsDtoMap.put(refKey, receiptProduction.getProducts());
                productionDistributionStocksDtoMap.put(refKey, receiptProduction.getDistributionStocks());
            }

            // Проверяем, получили ли мы полную страницу (если меньше 500, значит это последняя страница)
            if (responseDto.getValue().size() < top) {
                hasMoreData = false;
            } else {
                skip += top;
            }
        }

        log.info("Загружено {} документов на производство", receiptProductionMap.size());

        // Расчет
        for (Map.Entry<UUID, ReceiptProduction> entry : receiptProductionMap.entrySet()) {

            UUID uuid = entry.getKey();
            ReceiptProduction receipt = entry.getValue();

            String number = receipt.getNumber();
            String date = receipt.getDate().toString();
            String author = receipt.getAuthorKey().toString();

            // обработка

            for (ProductionItemsDto itemsDto : productionItemsDtoMap.get(uuid)) {

                String itemLinkKey = itemsDto.getLinkKey();

                boolean link = false;
                String nomenclatureKey = itemsDto.getNomenclatureKey().toString();

                if (whiteList.contains(nomenclatureKey)){
                    log.debug("Значение из белого списка -->{}", nomenclatureKey);
                    continue;
                }

                for (ProductionDistributionStocksDto distributionStocksDto : productionDistributionStocksDtoMap.get(uuid)) {

                    if (distributionStocksDto.getProductLinkKey().equals(itemLinkKey)) {
                        link = true;
                        log.debug("--> У продукции есть материал на списания! {}-->{}", distributionStocksDto.getProductLinkKey(), itemLinkKey);
                        break;
                    }
                }

                if (!link) {
                    addResultListOne(
                            author,
                            date,
                            nomenclatureKey,
                            number,
                            "paid"
                    );
                }
            }
        }

        return resultListOne;
    }

    private MonitoringProductionOutputResponseDto getResponseProduction(
            Integer top,
            Integer skip
    ) {
        log.info("------> Старт метода по поиску в 1с документов на Производство");

        LocalDateTime date = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        String url = String.format("/Document_СборкаЗапасов?" +
                        "$filter=Posted eq true" +
                        " and ЗаказПокупателя_Key eq guid'%s'" +
                        " and Date ge datetime'%s'" +
                        " and Date le datetime'%s'" +
                        "&" +
                        "$select=Автор_Key, Ref_Key, Number, Date, Продукция, Запасы, РаспределениеЗапасов&" +
                        "$orderby=Date desc&" +
                        "$top=%d&$skip=%d&" +
                        "$format=json",
                "00000000-0000-0000-0000-000000000000",
                date.minusMonths(1),
                date,
                top,
                skip);

        try {

            MonitoringProductionOutputResponseDto response = restClientConfig.restClient().get()
                    .uri(url)
                    .retrieve()
                    .body(MonitoringProductionOutputResponseDto.class);

            log.info("------> Конец метода по поиску в 1с документов на производство");
            return response;
        } catch (Exception e) {
            // Логирование ошибки
            log.error(
                    String.format("Ошибка при получении документов на производство"), String.valueOf(e)
            );
            return null;
        }
    }

    private void addResultListOne(String author, String date, String comment, String orderNumber, String status) {
        resultListOne.add(
                ProductionDetail.builder()
                        .author(author)
                        .date(date)
                        .comment(comment)
                        .orderNumber(orderNumber)
                        .status(status)
                        .build()
        );
    }

}
